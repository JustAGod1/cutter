package ru.justagod.plugin.util

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.PrimitiveKind
import ru.justagod.model.PrimitiveTypeReference
import ru.justagod.model.TypeReference

object PrimitivesAdapter {

    private val primitiveToWrapper = hashMapOf(
            PrimitiveKind.INT to PrimitiveEntry(PrimitiveKind.INT, ClassTypeReference(java.lang.Integer::class.java), "intValue"),
            PrimitiveKind.SHORT to PrimitiveEntry(PrimitiveKind.SHORT, ClassTypeReference(java.lang.Short::class.java), "shortValue"),
            PrimitiveKind.BYTE to PrimitiveEntry(PrimitiveKind.BYTE, ClassTypeReference(java.lang.Byte::class.java), "byteValue"),
            PrimitiveKind.BOOLEAN to PrimitiveEntry(PrimitiveKind.BOOLEAN, ClassTypeReference(java.lang.Boolean::class.java), "booleanValue"),
            PrimitiveKind.DOUBLE to PrimitiveEntry(PrimitiveKind.DOUBLE, ClassTypeReference(java.lang.Double::class.java), "doubleValue"),
            PrimitiveKind.CHAR to PrimitiveEntry(PrimitiveKind.CHAR, ClassTypeReference(java.lang.Character::class.java), "charValue"),
            PrimitiveKind.FLOAT to PrimitiveEntry(PrimitiveKind.FLOAT, ClassTypeReference(java.lang.Float::class.java), "floatValue"),
            PrimitiveKind.LONG to PrimitiveEntry(PrimitiveKind.LONG, ClassTypeReference(java.lang.Long::class.java), "longValue")
    )

    private val wrapperToWrapper = hashMapOf(
            ClassTypeReference(java.lang.Integer::class.java) to PrimitiveEntry(PrimitiveKind.INT, ClassTypeReference(Integer::class.java), "intValue"),
            ClassTypeReference(java.lang.Short::class.java) to PrimitiveEntry(PrimitiveKind.SHORT, ClassTypeReference(java.lang.Short::class.java), "shortValue"),
            ClassTypeReference(java.lang.Byte::class.java) to PrimitiveEntry(PrimitiveKind.BYTE, ClassTypeReference(java.lang.Byte::class.java), "byteValue"),
            ClassTypeReference(java.lang.Boolean::class.java) to PrimitiveEntry(PrimitiveKind.BOOLEAN, ClassTypeReference(java.lang.Boolean::class.java), "booleanValue"),
            ClassTypeReference(java.lang.Double::class.java) to PrimitiveEntry(PrimitiveKind.DOUBLE, ClassTypeReference(java.lang.Double::class.java), "doubleValue"),
            ClassTypeReference(java.lang.Character::class.java) to PrimitiveEntry(PrimitiveKind.CHAR, ClassTypeReference(java.lang.Character::class.java), "charValue"),
            ClassTypeReference(java.lang.Float::class.java) to PrimitiveEntry(PrimitiveKind.FLOAT, ClassTypeReference(java.lang.Float::class.java), "floatValue"),
            ClassTypeReference(java.lang.Long::class.java) to PrimitiveEntry(PrimitiveKind.LONG, ClassTypeReference(java.lang.Long::class.java), "longValue")
    )

    fun isWrapper(ref: ClassTypeReference) = ref in wrapperToWrapper

    fun getPrimitive(ref: ClassTypeReference) = wrapperToWrapper[ref]!!.primitive

    fun wrap(mv: MethodVisitor, kind: PrimitiveKind) {
        val entry = primitiveToWrapper[kind]!!
        val internalName = entry.wrapper.toASMType().internalName

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                internalName,
                "valueOf",
                "(${kind.asmType.descriptor})L$internalName;",
                false
        )
    }

    fun unwrap(mv: MethodVisitor, kind: PrimitiveKind) {
        val entry = primitiveToWrapper[kind]!!
        val internalName = entry.wrapper.toASMType().internalName

        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                internalName,
                entry.unwrapMethod,
                "()${kind.asmType.descriptor}",
                false
        )
    }

    fun getWrapperForPrimitive(kind: PrimitiveKind): ClassTypeReference {
        val entry = primitiveToWrapper[kind] ?: error("Cannot find entry for $kind")
        return entry.wrapper
    }

    private class PrimitiveEntry(
            val primitive: PrimitiveKind,
            val wrapper: ClassTypeReference,
            val unwrapMethod: String
    )
}

