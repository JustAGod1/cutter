package ru.justagod.cutter.model

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.io.Serializable
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass

/**
 * Type safe notation of bytecode types
 */
sealed class TypeReference : Serializable {
    abstract fun toASMType(): Type

    override fun toString() = toASMType().toString()
}

/**
 * Very handy class to get rid of internal/desc/dotted notation of classes in Java Bytecode.
 * Just use ClassTypeReference everywhere you can.
 *
 * It also makes code much more readable because you don't need to guess what does this random String means anymore
 */
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