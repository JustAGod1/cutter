package ru.justagod.processing.cutter.analization

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.LocalVariablesSorter
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodNode
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.*
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.model.MethodAtom
import ru.justagod.processing.cutter.model.ProjectModel
import ru.justagod.stackManipulation.*
import ru.justagod.utils.findMethod
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class LambdaAnalysisAugment(private val model: ProjectModel, config: CutterConfig) : AnalysisAugment(config) {

    private val id = AtomicLong()

    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        var modified = false

        val newMethods = arrayListOf<MethodNode>()
        val methods = context.info.node().methods

        remapLambdas(context)

        if (methods != null) {
            for (method in methods) {
                modified = modified or analyzeMethod(method, newMethods, context)

            }

            methods.addAll(newMethods)
        }

        return if (modified) MincerResultType.MODIFIED else MincerResultType.SKIPPED
    }

    private fun remapLambdas(
        context: WorkerContext<Unit, Unit>
    ) {

        val cache = hashMapOf<String, String>()
        context.info.node().methods ?: return
        for (method in context.info.node().methods) {

            method.instructions ?: continue
            for (insn in method.instructions) {
                if (insn is InvokeDynamicInsnNode) {
                    if (insn.bsm.name != "metafactory" && insn.bsm.name != "altMetafactory") continue
                    val implHandle = insn.bsmArgs[1] as Handle
                    if (ClassTypeReference.fromInternal(implHandle.owner) != context.name) continue

                    val desc = implHandle.name + implHandle.desc
                    val implMethod = context.info.node().findMethod(implHandle.name, implHandle.desc)
                    if (implMethod == null) {
                        if (desc in cache) {
                            insn.bsmArgs[1] = Handle(
                                implHandle.tag,
                                implHandle.owner,
                                cache[desc],
                                implHandle.desc,
                                implHandle.isInterface
                            )
                            continue
                        } else error("WTF")
                    }

                    if (lambdaImplPattern matches implMethod.name) {
                        val id = id.getAndIncrement()
                        val newName = "${implMethod.name.substringBeforeLast('\$')}$$id"
                        implMethod.name = newName
                        cache[desc] = newName
                        insn.bsmArgs[1] = Handle(
                            implHandle.tag,
                            implHandle.owner,
                            implMethod.name,
                            implHandle.desc,
                            implHandle.isInterface
                        )
                    }
                }
            }
        }

    }

    private fun analyzeMethod(
        method: MethodNode,
        methods: ArrayList<MethodNode>,
        context: WorkerContext<Unit, Unit>
    ): Boolean {
        method.instructions ?: return false
        var modified = false
        for (instruction in method.instructions) {
            if (instruction is InvokeDynamicInsnNode) {
                modified = modified or analyzeInvokeDynamic(method, methods, instruction, context)
            }
        }

        return modified
    }

    private fun analyzeInvokeDynamic(
        method: MethodNode,
        methods: ArrayList<MethodNode>,
        insn: InvokeDynamicInsnNode,
        context: WorkerContext<Unit, Unit>
    ): Boolean {
        if (insn.bsm.name != "metafactory" && insn.bsm.name != "altMetafactory") return false
        val clazz = ClassTypeReference.fromType(Type.getReturnType(insn.desc))
        val name = insn.name
        val desc = (insn.bsmArgs[0] as Type).descriptor
        val implHandle = insn.bsmArgs[1] as Handle
        if (ClassTypeReference.fromInternal(implHandle.owner) != context.name) return false
        val data =
            config.invocators.find { it.name == clazz && it.functionalMethod.name == name && it.functionalMethod.desc == desc }

        val implMethod = context.info.node().findMethod(implHandle.name, implHandle.desc)
        if (implMethod == null) {
            error("WTF")
        }

        if (implMethod.access and Opcodes.ACC_SYNTHETIC != 0) {
            val lambdaAtom = MethodAtom(context.name, implMethod)
            model.join(lambdaAtom, MethodAtom(context.name, method))
            if (data != null) {
                model.atom(lambdaAtom, data.sides)

                model.rememberLambdaMethod(lambdaAtom)
            }

        } else if (data != null) {
            val bridgeMethod = createBridgeMethod(implHandle, context)

            val bridgeAtom = MethodAtom(context.name, bridgeMethod)
            model.join(bridgeAtom, MethodAtom(context.name, method))
            model.atom(bridgeAtom, data.sides)

            model.rememberLambdaMethod(bridgeAtom)

            insn.bsmArgs[1] = Handle(
                Opcodes.H_INVOKESTATIC,
                context.name.internalName,
                bridgeMethod.name,
                bridgeMethod.desc,
                context.info.node().access and Opcodes.ACC_INTERFACE != 0
            )

            methods += bridgeMethod

        }
        return true

    }

    private fun createBridgeMethod(handle: Handle, context: WorkerContext<Unit, Unit>): MethodNode {
        val owner = ClassTypeReference.fromInternal(handle.owner)
        var name = "lambda\$bridge\$${handle.name}"
        val parameters = arrayListOf<TypeReference>()
        if (handle.tag != Opcodes.H_INVOKESTATIC && handle.tag != Opcodes.H_NEWINVOKESPECIAL) {
            parameters += ClassTypeReference.fromInternal(handle.owner)
        }
        Type.getArgumentTypes(handle.desc).forEach { parameters += it.toReference() }

        val returnType = if (handle.tag == Opcodes.H_NEWINVOKESPECIAL) owner
        else Type.getReturnType(handle.desc).toReference()

        val desc = buildString {
            append("(")
            parameters.joinTo(this, separator = "") { it.toASMType().descriptor }
            append(")")
            append(returnType.toASMType().descriptor)
        }
        while (context.info.node().findMethod(name, desc) != null) name += "_"
        val access = Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC
        val method = MethodNode(access, name, desc, null, null)

        val appender = SimpleAppender(LocalVariablesSorter(access, desc, method))

        if (handle.tag == Opcodes.H_NEWINVOKESPECIAL) {
            appender += NewInstanceInstruction(owner)
            appender += DupInstruction(owner)
        }

        for ((i, type) in parameters.withIndex()) {
            appender += LoadVariableInstruction(type, i)
        }
        when (handle.tag) {
            Opcodes.H_INVOKESTATIC -> {
                appender += InvokeStaticMethodInstruction(
                    owner, handle.name, handle.desc, handle.isInterface
                )
            }
            Opcodes.H_INVOKESPECIAL -> {
                appender += InvokeInstanceMethodInstruction(
                    owner, handle.name, handle.desc, Opcodes.INVOKESPECIAL, handle.isInterface
                )
            }
            Opcodes.H_INVOKEVIRTUAL -> {
                appender += InvokeInstanceMethodInstruction(
                    owner, handle.name, handle.desc, Opcodes.INVOKEVIRTUAL, handle.isInterface
                )
            }
            Opcodes.H_INVOKEINTERFACE -> {
                appender += InvokeInstanceMethodInstruction(
                    owner, handle.name, handle.desc, Opcodes.INVOKEINTERFACE, handle.isInterface
                )
            }
            Opcodes.H_NEWINVOKESPECIAL -> {
                appender += InvokeInstanceMethodInstruction(
                    owner, handle.name, handle.desc, Opcodes.INVOKESPECIAL, handle.isInterface
                )
            }
        }

        if (returnType is PrimitiveTypeReference && returnType.kind == PrimitiveKind.VOID) {
            appender += ReturnInstruction
        } else {
            appender += ReturnSomethingInstruction(returnType)
        }

        return method
    }


    companion object {

        private val lambdaImplPattern = "lambda\\$.+\\$\\d+".toRegex()
    }
}