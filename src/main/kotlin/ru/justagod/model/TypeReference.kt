package ru.justagod.model

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import kotlin.reflect.KClass

sealed class TypeReference {
    abstract fun toASMType(): Type

    override fun toString() = toASMType().toString()
}

data class ClassTypeReference(val name: String) : TypeReference() {

    constructor(klass: Class<*>): this(klass.name)
    constructor(klass: KClass<*>): this(klass.java.name)

    val path by lazy {
        name.split("[.$]".toRegex())
    }

    val simpleName by lazy {
        path.last()
    }

    override fun toASMType(): Type = Type.getType("L${name.replace(".", "/")};")

    fun internalName() = name.replace('.', '/')

    companion object {
        fun fromInternal(s: String) = ClassTypeReference(s.replace("/", "."))
        fun fromDesc(s: String) = fetchTypeReference(s) as ClassTypeReference
        fun fromDesc(t: Type) = fetchTypeReference(t) as ClassTypeReference
    }

}

data class PrimitiveTypeReference(val kind: PrimitiveKind) : TypeReference() {
    override fun toASMType(): Type = kind.asmType
}

data class ArrayTypeReference(val arrayType: TypeReference) : TypeReference() {
    override fun toASMType(): Type = Type.getType("[${arrayType.toASMType()}")
}

enum class PrimitiveKind(val asmType: Type, val arrayType: Int) {
    BYTE(Type.BYTE_TYPE, Opcodes.T_BYTE),
    SHORT(Type.SHORT_TYPE, Opcodes.T_SHORT),
    INT(Type.INT_TYPE, Opcodes.T_INT),
    LONG(Type.LONG_TYPE, Opcodes.T_LONG),
    FLOAT(Type.FLOAT_TYPE, Opcodes.T_FLOAT),
    DOUBLE(Type.DOUBLE_TYPE, Opcodes.T_DOUBLE),
    BOOLEAN(Type.BOOLEAN_TYPE, Opcodes.T_BOOLEAN),
    CHAR(Type.CHAR_TYPE, Opcodes.T_CHAR),
    VOID(Type.VOID_TYPE, 0)
}

val OBJECT_REFERENCE = ClassTypeReference("java.lang.Object")

fun fetchTypeReference(desc: String): TypeReference {
    val type = Type.getType(desc)

    return fetchTypeReference(type)
}


fun fetchTypeReference(type: Type): TypeReference {
    return when {
        type.sort == Type.OBJECT -> return ClassTypeReference(type.className)
        type.sort == Type.ARRAY -> return ArrayTypeReference(fetchTypeReference(type.descriptor.substring(1)))
        else -> PrimitiveTypeReference(when (type.sort) {
            Type.BYTE -> PrimitiveKind.BYTE
            Type.SHORT -> PrimitiveKind.SHORT
            Type.INT -> PrimitiveKind.INT
            Type.LONG -> PrimitiveKind.LONG
            Type.FLOAT -> PrimitiveKind.FLOAT
            Type.DOUBLE -> PrimitiveKind.DOUBLE
            Type.BOOLEAN -> PrimitiveKind.BOOLEAN
            Type.CHAR -> PrimitiveKind.CHAR
            Type.VOID -> PrimitiveKind.VOID
            else -> error("")

        })
    }
}
