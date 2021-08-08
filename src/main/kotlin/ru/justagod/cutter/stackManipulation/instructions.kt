package ru.justagod.cutter.stackManipulation

import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import ru.justagod.cutter.model.*
import ru.justagod.cutter.utils.AsmUtil
import kotlin.properties.Delegates

sealed class BytecodeInstruction {

    abstract fun accept(mv: VariablesManager)

    abstract fun transformStack(stack: BytecodeStack)
}

object IntToByteInstruction : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(Opcodes.I2B)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack -= PrimitiveTypeReference(PrimitiveKind.INT)
        stack += PrimitiveTypeReference(PrimitiveKind.BYTE)
    }

}


object IntToBooleanInstruction : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {}

    override fun transformStack(stack: BytecodeStack) {
        stack -= PrimitiveTypeReference(PrimitiveKind.INT)
        stack += PrimitiveTypeReference(PrimitiveKind.BOOLEAN)
    }

}

object IntXorInstruction : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(Opcodes.IXOR)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack -= PrimitiveTypeReference(PrimitiveKind.INT)
        stack -= PrimitiveTypeReference(PrimitiveKind.INT)
        stack += PrimitiveTypeReference(PrimitiveKind.INT)
    }

}

class NewVariableInstruction(private val type: TypeReference) : BytecodeInstruction() {

    var index: Int by Delegates.notNull()
        private set

    override fun accept(mv: VariablesManager) {
        index = mv.newLocal(type.toASMType())
    }

    override fun transformStack(stack: BytecodeStack) {}

}

class LoadVariableInstruction(private val type: TypeReference, private val index: () -> Int) : BytecodeInstruction() {

    constructor(type: TypeReference, variable: NewVariableInstruction) : this(type, { variable.index })
    constructor(type: TypeReference, index: Int) : this(type, { index })

    override fun accept(mv: VariablesManager) {
        mv.visitVarInsn(type.toASMType().getOpcode(ILOAD), index())
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.putValue(type)
    }
}

class PutVariableInstruction(private val type: TypeReference, private val index: () -> Int) : BytecodeInstruction() {

    constructor(type: TypeReference, variable: NewVariableInstruction) : this(type, { variable.index })
    constructor(type: TypeReference, index: Int) : this(type, { index })

    override fun accept(mv: VariablesManager) {
        mv.visitVarInsn(type.toASMType().getOpcode(ISTORE), index())
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(type)
    }
}

class IincVariableInstruction(private val amount: Int, private val index: () -> Int) : BytecodeInstruction() {

    constructor(amount: Int, variable: NewVariableInstruction) : this(amount, { variable.index })
    constructor(amount: Int, index: Int) : this(amount, { index })

    override fun accept(mv: VariablesManager) {
        mv.visitIincInsn(index(), amount)
    }

    override fun transformStack(stack: BytecodeStack) {
    }
}

class FieldGetInstruction(val owner: ClassTypeReference, val name: String, val type: TypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitFieldInsn(Opcodes.GETFIELD, owner.toASMType().internalName, name, type.toASMType().descriptor)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(owner)
        stack.putValue(type)
    }

}

class FieldPutStaticInstruction(private val owner: ClassTypeReference, private val name: String, private val type: TypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitFieldInsn(Opcodes.PUTSTATIC, owner.toASMType().internalName, name, type.toASMType().descriptor)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(type)
    }
}

class FieldGetStaticInstruction(private val owner: ClassTypeReference, private val name: String, private val type: TypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, owner.toASMType().internalName, name, type.toASMType().descriptor)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += type
    }
}

class FieldPutInstruction(private val owner: ClassTypeReference, private val name: String, private val type: TypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitFieldInsn(Opcodes.PUTFIELD, owner.toASMType().internalName, name, type.toASMType().descriptor)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(type)
        stack.removeValue(owner)
    }
}

class ArrayLengthInstruction(val type: ArrayTypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(Opcodes.ARRAYLENGTH)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(type)
        stack.putValue(PrimitiveTypeReference(PrimitiveKind.INT))
    }

}

class DupInstruction(private val type: TypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(Opcodes.DUP)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(type)
        stack.putValue(type)
        stack.putValue(type)
    }

}

object ByteToIntInstruction : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {}

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(PrimitiveTypeReference(PrimitiveKind.BYTE))
        stack.putValue(PrimitiveTypeReference(PrimitiveKind.INT))
    }
}

object ShortToIntInstruction : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {}

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(PrimitiveTypeReference(PrimitiveKind.SHORT))
        stack.putValue(PrimitiveTypeReference(PrimitiveKind.INT))
    }
}

object CharToIntInstruction : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {}

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(PrimitiveTypeReference(PrimitiveKind.CHAR))
        stack.putValue(PrimitiveTypeReference(PrimitiveKind.INT))
    }

}

class ArrayVariableLoadInstruction(val type: ArrayTypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(type.arrayType.toASMType().getOpcode(IALOAD))
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(PrimitiveTypeReference(PrimitiveKind.INT))
        stack.removeValue(type)
        stack.putValue(type.arrayType)
    }

}

class ArrayVariableStoreInstruction(val type: ArrayTypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(type.arrayType.toASMType().getOpcode(IASTORE))
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(type.arrayType)
        stack.removeValue(PrimitiveTypeReference(PrimitiveKind.INT))
        stack.removeValue(type)
    }
}

class LoadNullInstruction(private val type: TypeReference): BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(Opcodes.ACONST_NULL)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += type
    }
}

class CastInstruction(val from: TypeReference, val to: TypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitTypeInsn(Opcodes.CHECKCAST, to.toASMType().internalName)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(from)
        stack.putValue(to)
    }

}

class NewInstanceInstruction(
        private val type: ClassTypeReference
) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitTypeInsn(Opcodes.NEW, type.toASMType().internalName)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.putValue(type)
    }

}

class NewArrayInstruction(private val type: TypeReference) : BytecodeInstruction() {

    init {
        assert(type !is PrimitiveTypeReference)
    }

    override fun accept(mv: VariablesManager) {
        mv.visitTypeInsn(Opcodes.ANEWARRAY, type.toASMType().internalName)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(PrimitiveTypeReference(PrimitiveKind.INT))
        stack.putValue(ArrayTypeReference(type))
    }

}

class NewPrimitiveArrayInstruction(private val type: PrimitiveTypeReference) : BytecodeInstruction() {

    override fun accept(mv: VariablesManager) {
        mv.visitIntInsn(Opcodes.NEWARRAY, type.kind.arrayType)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(PrimitiveTypeReference(PrimitiveKind.INT))
        stack.putValue(ArrayTypeReference(type))
    }

}

class PopInstruction(private val type: TypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(Opcodes.POP)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack -= type
    }

}

class InvokeInstanceMethodInstruction(
        private val owner: TypeReference,
        private val name: String,
        private val desc: String,
        private val opcode: Int,
        private val itf: Boolean = opcode == Opcodes.INVOKEINTERFACE
) : BytecodeInstruction() {

    init {
        assert(opcode == INVOKESPECIAL || opcode == INVOKEVIRTUAL || opcode == INVOKEINTERFACE)
    }

    override fun accept(mv: VariablesManager) {
        mv.visitMethodInsn(opcode, owner.toASMType().internalName, name, desc, itf)
    }

    override fun transformStack(stack: BytecodeStack) {
        Type.getArgumentTypes(desc).reversed().forEach { stack.removeValue(it.toReference()) }
        stack.removeValue(owner)
        val returnType = Type.getReturnType(desc)
        if (returnType.sort != Type.VOID) stack.putValue(returnType.toReference())
    }
}



class TypeReturnInstruction(private val type: TypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        val opcode = if (type is PrimitiveTypeReference) {
            type.toASMType().getOpcode(Opcodes.IRETURN)
        } else Opcodes.ARETURN
        mv.visitInsn(opcode)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack -= type
    }

}

class InvokeStaticMethodInstruction(
        private val owner: TypeReference,
        private val name: String,
        private val desc: String,
        private val itf: Boolean = false
) : BytecodeInstruction() {

    override fun accept(mv: VariablesManager) {
        mv.visitMethodInsn(INVOKESTATIC, owner.toASMType().internalName, name, desc, itf)
    }

    override fun transformStack(stack: BytecodeStack) {
        Type.getArgumentTypes(desc).reversed().forEach { stack.removeValue(it.toReference()) }
        val returnType = Type.getReturnType(desc)
        if (returnType.sort != Type.VOID) stack.putValue(returnType.toReference())
    }
}

class ReturnSomethingInstruction(private val type: TypeReference): BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(type.toASMType().getOpcode(Opcodes.IRETURN))
    }

    override fun transformStack(stack: BytecodeStack) {
        stack -= type
    }

}

object ReturnInstruction : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(Opcodes.RETURN)
    }

    override fun transformStack(stack: BytecodeStack) {}

}

class StringLoadInstruction(private val value: String) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitLdcInsn(value)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += ClassTypeReference(String::class)
    }

}

class BooleanLoadInstruction(private val value: Boolean) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        AsmUtil.appendDecimal(mv, if (value) 1L else 0L)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += PrimitiveTypeReference(PrimitiveKind.BOOLEAN)
    }

}

class ByteLoadInstruction(private val value: Byte) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        AsmUtil.appendDecimal(mv, value.toLong())
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += PrimitiveTypeReference(PrimitiveKind.BYTE)
    }

}

class ShortLoadInstruction(private val value: Short) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        AsmUtil.appendDecimal(mv, value.toLong())
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += PrimitiveTypeReference(PrimitiveKind.SHORT)
    }

}

class IntLoadInstruction(private val value: Int) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        AsmUtil.appendDecimal(mv, value.toLong())
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += PrimitiveTypeReference(PrimitiveKind.INT)
    }

}

class CharLoadInstruction(private val value: Char) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        AsmUtil.appendDecimal(mv, value.toLong())
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += PrimitiveTypeReference(PrimitiveKind.CHAR)
    }

}

class FloatLoadInstruction(private val value: Float) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitLdcInsn(value)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += PrimitiveTypeReference(PrimitiveKind.FLOAT)
    }

}

class DoubleLoadInstruction(private val value: Double) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitLdcInsn(value)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += PrimitiveTypeReference(PrimitiveKind.DOUBLE)
    }

}

class LongLoadInstruction(private val value: Long) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        if (value == 0L) mv.visitInsn(Opcodes.LCONST_0)
        else if (value == 1L) mv.visitInsn(Opcodes.LCONST_1)
        else mv.visitLdcInsn(value)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += PrimitiveTypeReference(PrimitiveKind.LONG)
    }
}

class TypeLoadInstruction(private val type: TypeReference) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitLdcInsn(type.toASMType())
    }

    override fun transformStack(stack: BytecodeStack) {
        stack += ClassTypeReference("java.lang.Class")
    }

}

class LabelInstruction() : BytecodeInstruction() {

    val label = Label()

    override fun accept(mv: VariablesManager) {
        mv.visitLabel(label)
    }

    override fun transformStack(stack: BytecodeStack) {}

}

class DoubleIfInstruction(
        private val type1: TypeReference,
        private val type2: TypeReference,
        private val opcode: Int,
        private val label: () -> Label
) : BytecodeInstruction() {

    constructor(type1: TypeReference, type2: TypeReference, opcode: Int, label: LabelInstruction) : this(type1, type2, opcode, { label.label })
    constructor(type1: TypeReference, type2: TypeReference, opcode: Int, label: Label) : this(type1, type2, opcode, { label })

    override fun accept(mv: VariablesManager) {
        mv.visitJumpInsn(opcode, label())
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(type1)
        stack.removeValue(type2)

    }
}

class SingleIfInstruction(
        private val type: TypeReference,
        private val opcode: Int,
        private val label: () -> Label
) : BytecodeInstruction() {

    constructor(type: TypeReference, opcode: Int, label: LabelInstruction) : this(type, opcode, { label.label })
    constructor(type: TypeReference, opcode: Int, label: Label) : this(type, opcode, { label })

    override fun accept(mv: VariablesManager) {
        mv.visitJumpInsn(opcode, label())
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.removeValue(type)
    }

}

class GoToInstruction(private val label: () -> Label) : BytecodeInstruction() {

    constructor(label: LabelInstruction) : this({ label.label })
    constructor(label: Label) : this({ label })

    override fun accept(mv: VariablesManager) {
        mv.visitJumpInsn(Opcodes.GOTO, label())
    }

    override fun transformStack(stack: BytecodeStack) {}

}

class IntInsnInstruction(val opcode: Int) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(opcode)
    }

    override fun transformStack(stack: BytecodeStack) {
        stack.putValue(PrimitiveTypeReference(PrimitiveKind.INT))
    }

}

class AddDecimalsInstruction(val decimalsType: PrimitiveKind) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(decimalsType.asmType.getOpcode(IADD))
    }

    override fun transformStack(stack: BytecodeStack) {
        stack -= PrimitiveTypeReference(decimalsType)
        stack -= PrimitiveTypeReference(decimalsType)
        stack += PrimitiveTypeReference(decimalsType)
    }
}

class SubDecimalsInstruction(val decimalsType: PrimitiveKind) : BytecodeInstruction() {
    override fun accept(mv: VariablesManager) {
        mv.visitInsn(decimalsType.asmType.getOpcode(ISUB))
    }

    override fun transformStack(stack: BytecodeStack) {
        stack -= PrimitiveTypeReference(decimalsType)
        stack -= PrimitiveTypeReference(decimalsType)
        stack += PrimitiveTypeReference(decimalsType)
    }
}