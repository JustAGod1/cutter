package ru.justagod.utils

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

object CutterUtils {

    private val opcodes = hashMapOf<Int, String>()

    init {
        for (field in Opcodes::class.java.fields) {
            opcodes[field.get(null) as Int] = field.name
        }
    }

    fun opcodeToString(opcode: Int): String {
        return opcodes.getValue(opcode)
    }

    fun nodeToString(node: AbstractInsnNode): String {
        return when (node) {
            is MethodInsnNode -> opcodeToString(node.opcode) + " ${node.owner}.${node.name}${node.desc} itf = ${node.itf}"
            is FieldInsnNode -> opcodeToString(node.opcode) + " ${node.owner}.${node.name}${node.desc}"
            is TableSwitchInsnNode -> "~~~switch~~~"
            is LineNumberNode -> "${node.line}:"
            is LabelNode -> node.label.toString()
            is MultiANewArrayInsnNode -> opcodeToString(node.opcode) + " ${node.desc} ${node.dims}"
            is LdcInsnNode -> node.cst.javaClass.name + ": " + node.cst
            is TypeInsnNode -> opcodeToString(node.opcode) + " " + node.desc
            is VarInsnNode -> opcodeToString(node.opcode) + " " + node.`var`
            is FrameNode -> "~~~frame~~~"
            is InvokeDynamicInsnNode -> opcodeToString(node.opcode) + "${node.name}${node.desc} [${node.bsm.owner}.${node.bsm.name}${node.bsm.desc} tag = ${node.bsm.tag} itf = ${node.bsm.isInterface}] {${node.bsmArgs.joinToString()}}"
            is JumpInsnNode -> opcodeToString(node.opcode) + " " + node.label.label
            is InsnNode -> opcodeToString(node.opcode)
            is LookupSwitchInsnNode -> "~~~lookup~~~"
            else -> error("")

        }

    }
}