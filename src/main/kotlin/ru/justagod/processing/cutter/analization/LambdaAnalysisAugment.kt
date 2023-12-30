package ru.justagod.processing.cutter.analization

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.LocalVariablesSorter
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import ru.justagod.stackManipulation.*
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.*
import ru.justagod.model.factory.BytecodeModelFactory
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.model.ClassAtom
import ru.justagod.processing.cutter.model.MethodAtom
import ru.justagod.processing.cutter.model.MethodBodyAtom
import ru.justagod.processing.cutter.model.ProjectModel
import ru.justagod.utils.findMethod
import ru.justagod.utils.nope
import java.util.concurrent.atomic.AtomicLong

class LambdaAnalysisAugment(private val model: ProjectModel, config: CutterConfig) : AnalysisAugment(config) {

    private val id = AtomicLong()
    private val KOTLIN_FUNCTION_BASE = ClassTypeReference("kotlin.jvm.internal.FunctionBase")

    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        var modified = false

        val newMethods = arrayListOf<MethodNode>()
        val methods = context.info.node().methods

        remapLambdas(context)

        val createdBridges = hashMapOf<HandleWrapper, MethodNode>()
        if (methods != null) {
            for (method in methods) {
                modified = modified or analyzeMethod(method, newMethods, createdBridges, context)

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
        bridges: MutableMap<HandleWrapper, MethodNode>,
        context: WorkerContext<Unit, Unit>
    ): Boolean {
        method.instructions ?: return false

        var modified = false
        for (instruction in method.instructions) {
            if (instruction is InvokeDynamicInsnNode) {
                modified = modified or analyzeInvokeDynamic(method, methods, instruction, bridges, context)
            }
            modified = modified or analyzePotentialLambda(method, instruction, context)
        }

        return modified
    }

    private fun analyzePotentialLambda(method: MethodNode, instruction: AbstractInsnNode, context: WorkerContext<Unit, Unit>): Boolean {
        if (instruction is FieldInsnNode
            && instruction.opcode == Opcodes.GETSTATIC
            && instruction.name == "INSTANCE") {
            val potentialLambda = ClassTypeReference.fromInternal(instruction.owner)
            if (context.mincer.inheritance.isChild(potentialLambda, KOTLIN_FUNCTION_BASE)) {
                model.join(ClassAtom(potentialLambda), MethodBodyAtom(context.name, method))
                return true
            }
        } else if(instruction is TypeInsnNode && instruction.opcode == Opcodes.NEW) {
            val potentialLambda = ClassTypeReference.fromInternal(instruction.desc)
            if(context.name != potentialLambda
                && context.mincer.inheritance.isChild(potentialLambda, KOTLIN_FUNCTION_BASE)) {
                model.join(ClassAtom(potentialLambda), MethodBodyAtom(context.name, method))
                return true
            }
        }
        return false
    }

    private fun analyzeInvokeDynamic(
        method: MethodNode,
        methods: ArrayList<MethodNode>,
        insn: InvokeDynamicInsnNode,
        bridges: MutableMap<HandleWrapper, MethodNode>,
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

        if (implMethod.access and Opcodes.ACC_SYNTHETIC != 0
            || implMethod.name.contains("-")
            || implMethod.name.contains("\$lambda\$")) {
            val lambdaAtom = MethodAtom(context.name, implMethod)
            model.join(lambdaAtom, MethodBodyAtom(context.name, method))
            if (data != null) {
                model.atom(MethodBodyAtom(lambdaAtom), data.sides)
            }

        } else if (data != null) {

            val wrapper = HandleWrapper(implHandle)
            val bridgeMethod = if (wrapper in bridges) {
                bridges[wrapper]!!
            } else {
                val bridgeMethod = createBridgeMethod(implHandle, context)
                methods += bridgeMethod
                bridges[wrapper] = bridgeMethod
                bridgeMethod
            }

            val bridgeAtom = MethodAtom(context.name, bridgeMethod)
            model.join(bridgeAtom, MethodAtom(context.name, method))
            model.atom(MethodBodyAtom(bridgeAtom), data.sides)

            insn.bsmArgs[1] = Handle(
                Opcodes.H_INVOKESTATIC,
                context.name.internalName,
                bridgeMethod.name,
                bridgeMethod.desc,
                context.info.node().access and Opcodes.ACC_INTERFACE != 0
            )


        }
        return true

    }

    private fun createBridgeMethod(
        handle: Handle,
        context: WorkerContext<Unit, Unit>
    ): MethodNode {
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

    private class HandleWrapper(val handle: Handle) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HandleWrapper

            if (handle.tag != other.handle.tag) return false
            if (handle.isInterface != other.handle.isInterface) return false
            if (handle.desc != other.handle.desc) return false
            if (handle.owner != other.handle.owner) return false
            if (handle.name != other.handle.name) return false

            return true
        }

        override fun hashCode(): Int {
            var hash = 0
            hash += handle.tag.hashCode()
            hash *= 256
            hash += handle.isInterface.hashCode()
            hash *= 256
            hash += handle.desc.hashCode()
            hash *= 256
            hash += handle.owner.hashCode()
            hash *= 256
            hash += handle.name.hashCode()
            hash *= 256
            return hash
        }
    }
}