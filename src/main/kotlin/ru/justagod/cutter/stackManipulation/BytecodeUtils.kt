package ru.justagod.cutter.stackManipulation

import org.objectweb.asm.ClassWriter
import ru.justagod.cutter.model.ArrayTypeReference
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.model.PrimitiveKind.INT
import ru.justagod.cutter.model.PrimitiveTypeReference
import ru.justagod.cutter.model.TypeReference
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.LocalVariablesSorter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

object BytecodeUtils {

    fun nodeToByteArray(node: ClassNode): ByteArray {
        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
        node.accept(writer)
        return writer.toByteArray()
    }

    fun makeFor(appender: BytecodeAppender,
                maximumFetcher: InstructionSet,
                body: (NewVariableInstruction) -> Unit
    ) {
        val variable = NewVariableInstruction(PrimitiveTypeReference(INT))
        appender += variable
        appender += IntInsnInstruction(Opcodes.ICONST_0)
        appender += PutVariableInstruction(PrimitiveTypeReference(INT), variable)
        val forStart = LabelInstruction()
        appender += forStart
        val forEnd = LabelInstruction()
        appender += LoadVariableInstruction(PrimitiveTypeReference(INT), variable)
        appender += maximumFetcher
        appender += DoubleIfInstruction(PrimitiveTypeReference(INT), PrimitiveTypeReference(INT), Opcodes.IF_ICMPGE, forEnd)
        body.invoke(variable)
        appender += IincVariableInstruction(1, variable)
        appender += GoToInstruction(forStart)
        appender += forEnd
    }

    fun makeArrayFor(type: ArrayTypeReference, appender: BytecodeAppender, body: ArrayWorker.(NewVariableInstruction) -> Unit) {
        val variable = NewVariableInstruction(type)
        appender += variable
        appender += PutVariableInstruction(type, variable)
        val fetcher = InstructionSet.create(
                LoadVariableInstruction(type, variable),
                ArrayLengthInstruction(type)
        )
        val internalBody: (NewVariableInstruction) -> Unit = { i ->

            val worker = ArrayWorker(variable, i, type)
            worker.body(i)
        }
        makeFor(appender, fetcher, internalBody)
    }

    fun makeDefaultInstance(type: ClassTypeReference, appender: BytecodeAppender) =
        makeInstance(type, appender, "()V", InstructionSet.EMPTY)

    fun makeInstance(type: ClassTypeReference, appender: BytecodeAppender, desc: String, args: InstructionSet) {
        appender += NewInstanceInstruction(type)
        appender += DupInstruction(type)
        appender += args
        appender += InvokeInstanceMethodInstruction(type, "<init>", desc, Opcodes.INVOKESPECIAL)
    }

    fun makeArray(type: TypeReference, appender: BytecodeAppender) {
        appender += if (type is PrimitiveTypeReference) {
            NewPrimitiveArrayInstruction(type)
        } else {
            NewArrayInstruction(type)
        }
    }

    fun makeMethod(node: ClassNode, name: String, desc: String, access: Int, signature: String? = null, exceptions: Array<String>? = null): BytecodeAppender {
        return SimpleAppender(makeMethodSimple(node, name, desc, access, signature, exceptions), BytecodeStack())
    }

    fun makeMethodSimple(node: ClassNode, name: String, desc: String, access: Int, signature: String? = null, exceptions: Array<String>? = null): LocalVariablesSorter {
        val method = MethodNode(Opcodes.ASM6, name, desc, signature, exceptions)
        method.access = access
        method.instructions = InsnList()
        val visitor = LocalVariablesSorter(access, desc, method)
        if (node.methods == null) {
            node.methods = mutableListOf()
        }
        node.methods.add(method)
        return visitor
    }

    fun makeDefaultConstructor(node: ClassNode) {
        val appender = makeMethod(node, "<init>", "()V", Opcodes.ACC_PUBLIC)
        appender += LabelInstruction()
        val superType = ClassTypeReference(node.superName.replace("[\\\\/]".toRegex(), "."))
        appender += LoadVariableInstruction(superType, 0)
        appender += InvokeInstanceMethodInstruction(superType, "<init>", "()V", Opcodes.INVOKESPECIAL)
        appender += LabelInstruction()
        appender += ReturnInstruction
        appender += LabelInstruction()
    }

    class ArrayWorker(private val arrVariable: NewVariableInstruction, private val iVariable: NewVariableInstruction, private val type: ArrayTypeReference) {
        fun putValue(appender: BytecodeAppender) {
            appender += LoadVariableInstruction(type, arrVariable)
            appender += LoadVariableInstruction(PrimitiveTypeReference(INT), iVariable)
            appender += ArrayVariableLoadInstruction(type)
        }
    }
}