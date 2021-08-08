package ru.justagod.stackManipulation

import java.util.*

class InstructionSet(val instructions: List<BytecodeInstruction>, validate: Boolean = false) {

    init {
        if (validate) {
            val stack = BytecodeStack()
            instructions.forEach { it.transformStack(stack) }
        }
    }

    companion object {
        fun create(vararg instructions: BytecodeInstruction) = InstructionSet(instructions.toList())

        val EMPTY = InstructionSet(emptyList())
    }

    class Builder(private val stack: BytecodeStack) : BytecodeAppender {

        constructor(): this(BytecodeStack())

        override fun makeSetBuilder(): Builder = Builder(stack.copy())

        private val buffer = LinkedList<BytecodeInstruction>()

        override fun append(instruction: BytecodeInstruction) {
            instruction.transformStack(stack)
            buffer += instruction
        }

        fun build(): InstructionSet = InstructionSet(buffer)

    }
}