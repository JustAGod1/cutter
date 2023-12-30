package ru.justagod.model

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.io.Serializable
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass

sealed class TypeReference : Serializable {
    abstract fun toASMType(): Type

    abstract fun size(): Int
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



    val internalName by lazy { name.replace('.', '/') }

    fun desc() = toASMType().descriptor

    override fun toASMType(): Type = Type.getType("L${name.replace(".", "/")};")
    override fun size(): Int = 8

    companion object {
        fun fromInternal(str: String) = ClassTypeReference(str.replace("/", "."))
        fun fromType(type: Type): ClassTypeReference {
            if (type.sort != Type.OBJECT) throw IllegalArgumentException()
            return fromInternal(type.internalName)
        }
        fun fromDesc(desc: String) = fromType(Type.getType(desc))
        fun fromFilePath(path: String) =
            if (path.endsWith(".class"))
                ClassTypeReference(path.dropLast(6).replace('/', '.').replace('\\', '.'))
            else
                ClassTypeReference(path.replace('/', '.').replace('\\', '.'))
    }

}

data class PrimitiveTypeReference(val kind: PrimitiveKind) : TypeReference() {
    override fun toASMType(): Type = kind.asmType

    companion object {
        val BYTE = PrimitiveTypeReference(PrimitiveKind.BYTE)
        val SHORT = PrimitiveTypeReference(PrimitiveKind.SHORT)
        val INT = PrimitiveTypeReference(PrimitiveKind.INT)
        val LONG = PrimitiveTypeReference(PrimitiveKind.LONG)
        val FLOAT = PrimitiveTypeReference(PrimitiveKind.FLOAT)
        val DOUBLE = PrimitiveTypeReference(PrimitiveKind.DOUBLE)
        val BOOLEAN = PrimitiveTypeReference(PrimitiveKind.BOOLEAN)
        val CHAR = PrimitiveTypeReference(PrimitiveKind.CHAR)
        val VOID = PrimitiveTypeReference(PrimitiveKind.VOID)
    }

    override fun size(): Int = kind.size

}

data class ArrayTypeReference(val arrayType: TypeReference) : TypeReference() {
    override fun toASMType(): Type = Type.getType("[${arrayType.toASMType()}")

    override fun size(): Int = 8
}

enum class PrimitiveKind(val asmType: Type, val arrayType: Int, val size: Int) {
    BYTE(Type.BYTE_TYPE, Opcodes.T_BYTE, 1),
    SHORT(Type.SHORT_TYPE, Opcodes.T_SHORT, 2),
    INT(Type.INT_TYPE, Opcodes.T_INT, 4),
    LONG(Type.LONG_TYPE, Opcodes.T_LONG, 8),
    FLOAT(Type.FLOAT_TYPE, Opcodes.T_FLOAT, 4),
    DOUBLE(Type.DOUBLE_TYPE, Opcodes.T_DOUBLE, 8),
    BOOLEAN(Type.BOOLEAN_TYPE, Opcodes.T_BOOLEAN, 1),
    CHAR(Type.CHAR_TYPE, Opcodes.T_CHAR, 2),
    VOID(Type.VOID_TYPE, 0, 0)
}

val OBJECT_REFERENCE = ClassTypeReference("java.lang.Object")
val STRING_REFERENCE = ClassTypeReference("java.lang.String")

fun Type.toReference() = fetchTypeReference(this)

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