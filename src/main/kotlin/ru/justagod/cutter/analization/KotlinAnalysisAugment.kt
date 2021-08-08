package ru.justagod.processing.cutter.analization

import kotlinx.metadata.Flag
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.factory.BytecodeModelFactory
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.model.ClassAtom
import ru.justagod.processing.cutter.model.FieldAtom
import ru.justagod.processing.cutter.model.MethodAtom
import ru.justagod.processing.cutter.model.ProjectModel
import ru.justagod.utils.findMethod

class KotlinAnalysisAugment(private val model: ProjectModel, config: CutterConfig) : AnalysisAugment(config) {
    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        for (method in context.info.node().methods) {
            if (method.access and Opcodes.ACC_SYNTHETIC == 0) continue

            val me = MethodAtom(context.name, method)
            if (method.name.endsWith("\$default")) {
                val owner = lastInvokedMethod(method) ?: continue

                model.join(me, owner)
            } else if (method.name.startsWith("access\$")) {
                val owner = lastInvokedMethod(method) ?: lastInvokedField(method) ?: continue

                model.join(me, owner)
            }

            if (method.access and Opcodes.ACC_BRIDGE != 0) {
                val delegate = lastInvokedMethod(method) ?: continue

                if (delegate.owner == context.name) model.join(delegate, me)
            }
        }


        inspectKotlinMetadata(context)

        return MincerResultType.SKIPPED
    }


    private fun inspectKotlinMetadata(context: WorkerContext<Unit, Unit>) {
        val meta =
            context.info.node().visibleAnnotations?.find { it.desc == ClassTypeReference(Metadata::class).desc() }
        if (meta == null) return
        var k: Int? = null
        var mv: IntArray? = null
        var bv: IntArray? = null
        var d1: Array<String>? = null
        var d2: Array<String>? = null
        var xs: String? = null
        var pn: String? = null
        var xi: Int? = null

        for (i in 0 until meta.values.size step 2) {
            when (meta.values[i] as String) {
                "k" -> k = meta.values[i + 1] as Int
                "mv" -> mv = (meta.values[i + 1] as List<Int>).toIntArray()
                "bv" -> bv = (meta.values[i + 1] as List<Int>).toIntArray()
                "d1" -> d1 = (meta.values[i + 1] as List<String>).toTypedArray()
                "d2" -> d2 = (meta.values[i + 1] as List<String>).toTypedArray()
                "xs" -> xs = meta.values[i + 1] as String
                "pn" -> pn = meta.values[i + 1] as String
                "xi" -> xi = meta.values[i + 1] as Int
            }
        }

        val header = KotlinClassHeader(
            kind = k,
            metadataVersion = mv,
            bytecodeVersion = bv,
            data1 = d1,
            data2 = d2,
            extraString = xs,
            packageName = pn,
            extraInt = xi
        )

        val metadata = KotlinClassMetadata.read(header)!!

        if (metadata is KotlinClassMetadata.Class) {
            val klass = metadata.toKmClass()

            for (function in klass.functions) {
                if (Flag.Function.IS_DELEGATION.invoke(function.flags)) {
                    for (method in context.info.node().methods) {
                        if (method.name == function.name) {
                            val itf = findInInterfaces(context, method) ?: continue

                            model.join(
                                MethodAtom(context.name, method),
                                MethodAtom(itf, method)
                            )
                        }
                    }
                }
            }

            val companion = klass.companionObject
            if (companion != null) {
                model.join(
                    ClassAtom(ClassTypeReference(context.name.name + "$" + companion)),
                    ClassAtom(context.name)
                )
            }

            for (nestedClass in klass.nestedClasses) {
                model.join(
                    ClassAtom(ClassTypeReference(context.name.name + "$" + nestedClass)),
                    ClassAtom(context.name)
                )
            }
        }
    }

    private fun findInInterfaces(context: WorkerContext<Unit, Unit>, method: MethodNode): ClassTypeReference? {
        for (itf in context.info.node().interfaces) {
            val ref = ClassTypeReference.fromInternal(itf)

            val itfNode = try {
                context.mincer.nodes.makeNode(ref)
            } catch (e: BytecodeModelFactory.BytecodeNotFoundException) {
                continue
            }

            if (itfNode.findMethod(method.name, method.desc) != null) return ref
        }

        return null
    }

    private fun lastInvokedMethod(method: MethodNode): MethodAtom? {
        var insn: MethodInsnNode? = null
        for (instruction in method.instructions) {
            if (instruction is MethodInsnNode) insn = instruction
        }

        insn ?: return null

        return MethodAtom(ClassTypeReference.fromInternal(insn.owner), insn.name, insn.desc)
    }

    private fun lastInvokedField(method: MethodNode): FieldAtom? {
        var insn: FieldInsnNode? = null
        for (instruction in method.instructions) {
            if (instruction is FieldInsnNode) insn = instruction
        }

        insn ?: return null

        return FieldAtom(ClassTypeReference.fromInternal(insn.owner), insn.name)
    }


}