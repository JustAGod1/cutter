package ru.justagod.utils

import org.objectweb.asm.Type
import java.lang.StringBuilder

class SmartMethodType(
        val args: List<Type>,
        val returnType: Type,
        private val owner: Type?
) {

    constructor(type: Type, owner: Type?) : this(type.argumentTypes.toList(), type.returnType, owner)

    val descriptor: String by lazy {
        val builder = StringBuilder()
        builder.append("(")
        for (arg in args) {
            builder.append(arg.descriptor)
        }
        builder.append(")")
        builder.append(returnType.descriptor)
        builder.toString()
    }

    fun getArgumentsLength(): Int {
        return if (owner != null) {
            args.size + 1
        } else args.size;
    }

    fun getArguments(): List<Type> {
        return if (owner != null) {
            listOf(owner) + args
        } else args
    }
}