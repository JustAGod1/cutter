package ru.justagod.stackManipulation

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.LocalVariablesSorter

abstract class VariablesManager(api: Int, mv: MethodVisitor?): MethodVisitor(api, mv) {

    abstract fun newLocal(type: Type): Int
}

class DelegatedVariablesManager(private val sorter: LocalVariablesSorter): VariablesManager(Opcodes.ASM9, sorter) {
    override fun newLocal(type: Type): Int {
        return sorter.newLocal(type)
    }

}

class SimpleAppender(private val mv: VariablesManager, private val stack: BytecodeStack) : BytecodeAppender {

    constructor(mv: LocalVariablesSorter, stack: BytecodeStack): this(DelegatedVariablesManager(mv), stack)
    constructor(mv: LocalVariablesSorter): this(DelegatedVariablesManager(mv), BytecodeStack())

    override fun append(instruction: BytecodeInstruction) {
        instruction.transformStack(stack)
        instruction.accept(mv)
    }

    override fun makeSetBuilder(): InstructionSet.Builder = InstructionSet.Builder(stack.copy())
}