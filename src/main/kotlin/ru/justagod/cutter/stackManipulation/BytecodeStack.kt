package ru.justagod.cutter.stackManipulation

import ru.justagod.cutter.model.TypeReference
import java.util.*

class BytecodeStack private constructor(private val content: Stack<TypeReference>) {

    constructor() : this(Stack())

    constructor(vararg values: TypeReference) : this() {
        for (value in values) {
            this += value
        }
    }

    constructor(values: List<TypeReference>) : this() {
        for (value in values) {
            this += value
        }
    }

    operator fun plusAssign(type: TypeReference) {
        putValue(type)
    }

    operator fun minusAssign(type: TypeReference) {
        removeValue(type)
    }

    fun putValue(type: TypeReference) {
        content.push(type)
    }

    fun removeValue(type: TypeReference) {
        if (content.empty())
            error("Type stack is empty:(")
        val top = content.peek()
        if (top != type)
            error("Illegal type on top of the stack.\nExpected: $type. Actual: $top")
        content.pop()
    }

    fun copy(): ru.justagod.cutter.stackManipulation.BytecodeStack = ru.justagod.cutter.stackManipulation.BytecodeStack(content.copy())

    fun <E> Stack<E>.copy(): Stack<E> {
        val result = Stack<E>()
        for (element in this.elements()) {
            result += element
        }
        return result
    }
}