package ru.justagod.utils


import org.objectweb.asm.*
import org.objectweb.asm.commons.LocalVariablesSorter
import org.objectweb.asm.tree.*
import ru.justagod.stackManipulation.*
import ru.justagod.model.*
import ru.justagod.model.TypeReference
import ru.justagod.model.factory.ModelFactory
import java.lang.reflect.Field
import java.lang.reflect.Method

object AsmUtil {

    fun makeForI(mv: LocalVariablesSorter, body: LocalVariablesSorter.(Int) -> Unit, max: LocalVariablesSorter.() -> Unit) {
        val variable = mv.newLocal(Type.getType("I"))
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitVarInsn(Opcodes.ISTORE, variable)
        val forStart = Label()
        val forEnd = Label()
        mv.visitLabel(forStart)
        mv.visitVarInsn(Opcodes.ILOAD, variable)
        mv.max()
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, forEnd)
        mv.body(variable)
        mv.visitIincInsn(variable, 1)
        mv.visitJumpInsn(Opcodes.GOTO, forStart)
        mv.visitLabel(forEnd)
    }

    fun makeForI(appender: BytecodeAppender, max: InstructionSet, body: (NewVariableInstruction) -> Unit) {
        val iVar = NewVariableInstruction(PrimitiveTypeReference(PrimitiveKind.INT))
        appender += iVar
        appender += IntLoadInstruction(0)
        appender += PutVariableInstruction(PrimitiveTypeReference(PrimitiveKind.INT), iVar)
        val forStart = LabelInstruction()
        val forEnd = LabelInstruction()
        appender += forStart
        appender += LoadVariableInstruction(PrimitiveTypeReference(PrimitiveKind.INT), iVar)
        appender += max
        appender += DoubleIfInstruction(PrimitiveTypeReference(PrimitiveKind.INT), PrimitiveTypeReference(PrimitiveKind.INT), Opcodes.IF_ICMPGE, forEnd)
        body.invoke(iVar)
        appender += IincVariableInstruction(1, iVar)
        appender += GoToInstruction(forStart)
        appender += forEnd
    }


    fun appendDecimal(mv: MethodVisitor, value: Long) {
        when (value) {
            in -1..5 -> mv.visitInsn(Opcodes.ICONST_0 + value.toInt())
            in Byte.MIN_VALUE..Byte.MAX_VALUE -> mv.visitIntInsn(Opcodes.BIPUSH, value.toInt())
            in Short.MIN_VALUE..Short.MAX_VALUE -> mv.visitIntInsn(Opcodes.SIPUSH, value.toInt())
            else -> mv.visitLdcInsn(value)
        }

    }

    fun getSuperClass(bytecode: ByteArray): String? {
        var result: String? = null
        accept(bytecode, object : ClassVisitor(Opcodes.ASM5) {
            override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {

                result = superName
            }
        })
        return result
    }

    fun accept(bytecode: ByteArray, visitor: ClassVisitor, flags: Int = 0) {
        val reader = ClassReader(bytecode)
        reader.accept(visitor, flags)
    }

    private fun getFieldViaReflect(ref: ClassTypeReference, name: String, appender: BytecodeAppender) {
        appender += StringLoadInstruction(ref.name)
        appender += InvokeStaticMethodInstruction(ClassTypeReference(Class::class), "forName", "(Ljava/lang/String;)Ljava/lang/Class;")
        appender += StringLoadInstruction(name)
        appender += InvokeInstanceMethodInstruction(
                ClassTypeReference(Class::class),
                "getDeclaredField",
                "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                Opcodes.INVOKEVIRTUAL
        )
        appender += DupInstruction(ClassTypeReference(Field::class))
        appender += IntLoadInstruction(1)
        appender += IntToBooleanInstruction
        appender += InvokeInstanceMethodInstruction(
                ClassTypeReference(Field::class),
                "setAccessible",
                "(Z)V",
                Opcodes.INVOKEVIRTUAL
        )

    }

    private fun getDeclaredMethod(
            ref: ClassTypeReference,
            name: String,
            args: List<TypeReference>,
            appender: BytecodeAppender
    ) {
        val arrayType = ArrayTypeReference(ClassTypeReference(Class::class))

        appender += IntLoadInstruction(args.size)
        appender += NewArrayInstruction(ClassTypeReference(Class::class))

        val typesArr = NewVariableInstruction(arrayType)
        appender += typesArr
        appender += PutVariableInstruction(arrayType, typesArr)

        for (i in args.indices) {
            appender += LoadVariableInstruction(arrayType, typesArr)
            appender += IntLoadInstruction(i)
            PrimitivesAdapter.getClass(args[i], appender)
            appender += ArrayVariableStoreInstruction(arrayType)
        }

        appender += StringLoadInstruction(ref.name)
        appender += InvokeStaticMethodInstruction(ClassTypeReference(Class::class), "forName", "(Ljava/lang/String;)Ljava/lang/Class;")
        appender += StringLoadInstruction(name)
        appender += LoadVariableInstruction(arrayType, typesArr)
        appender += InvokeInstanceMethodInstruction(
                ClassTypeReference(Class::class),
                "getDeclaredMethod",
                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                Opcodes.INVOKEVIRTUAL
        )

        appender += DupInstruction(ClassTypeReference(Method::class))
        appender += IntLoadInstruction(1)
        appender += IntToBooleanInstruction
        appender += InvokeInstanceMethodInstruction(
                ClassTypeReference(Method::class),
                "setAccessible",
                "(Z)V",
                Opcodes.INVOKEVIRTUAL
        )
    }

    fun invokeMethodViaReflect(
            ref: ClassTypeReference,
            name: String,
            args: List<TypeReference>,
            appender: BytecodeAppender,
            value: InstructionSet
    ) {
        getDeclaredMethod(ref, name, args, appender)
        appender += value

        appender += InvokeInstanceMethodInstruction(
                ClassTypeReference(Method::class),
                "invoke",
                "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                Opcodes.INVOKEVIRTUAL
        )
    }

    fun setFieldValueViaReflect(ref: ClassTypeReference, name: String, appender: BytecodeAppender, value: InstructionSet) {
        getFieldViaReflect(ref, name, appender)
        appender += value

        appender += InvokeInstanceMethodInstruction(
                ClassTypeReference(Field::class),
                "set",
                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                Opcodes.INVOKEVIRTUAL
        )
    }

    fun getFieldValueViaReflect(ref: ClassTypeReference, name: String, appender: BytecodeAppender, value: InstructionSet) {
        getFieldViaReflect(ref, name, appender)
        appender += value

        appender += InvokeInstanceMethodInstruction(
                ClassTypeReference(Field::class),
                "get",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                Opcodes.INVOKEVIRTUAL
        )
    }

    fun getNumberInsn(number: Int): AbstractInsnNode {
        return if (number >= -1 && number <= 5) {
            InsnNode(number + 5)
        } else if (number >= -128 && number < 127) {
            IntInsnNode(Opcodes.BIPUSH, number)
        } else if (number >= -32768 && number <= 32767) {
            return IntInsnNode(Opcodes.SIPUSH, number)
        } else {
            LdcInsnNode(number)
        }
    }

    fun getNumberInsn(number: Long): AbstractInsnNode {
        return if (number == 0L || number == 1L) {
            InsnNode(number.toInt() + 9)
        } else {
            LdcInsnNode(number)
        }
    }

    fun getNumberInsn(number: Float): AbstractInsnNode {
        return LdcInsnNode(number)
    }

    fun getNumberInsn(number: Double): AbstractInsnNode {
        return if (number == 0.0 || number == 1.0) {
            InsnNode(number.toInt() + 14)
        } else {
            LdcInsnNode(number)
        }
    }
}

fun InstructionSet.toInsnList(): InsnList {
    val m = MethodNode(0, "fake", "()V", null, null)
    val appender = SimpleAppender(LocalVariablesSorter(Opcodes.ASM6, "()V", m))
    appender += this

    return m.instructions
}

fun ModelFactory.makeModel(clazz: Class<*>): ClassModel = this.makeModel(ClassTypeReference(clazz), null)


fun Type.isArray() = this.descriptor.startsWith("[")

val Type.arrayType: Type?
    get() = if (isArray()) Type.getType(this.descriptor.substring(1)) else null

fun ClassNode.findMethod(name: String, desc: String) = this.methods?.find { it.name == name && it.desc == desc }

fun ClassNode.findField(name: String) = this.fields?.find { it.name == name }
