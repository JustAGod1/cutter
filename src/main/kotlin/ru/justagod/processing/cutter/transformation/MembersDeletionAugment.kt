package ru.justagod.processing.cutter.transformation

import org.objectweb.asm.Type
import org.objectweb.asm.commons.LocalVariablesSorter
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.PrimitiveKind
import ru.justagod.model.PrimitiveTypeReference
import ru.justagod.model.toReference
import ru.justagod.processing.cutter.base.MincerAugment
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.model.*
import ru.justagod.processing.cutter.transformation.validation.ValidationResult
import ru.justagod.stackManipulation.*
import ru.justagod.utils.PrimitivesAdapter
import ru.justagod.utils.containsAny
import ru.justagod.utils.nope

class MembersDeletionAugment(private val config: CutterConfig, private val model: ProjectModel) :
    MincerAugment<Unit, ValidationResult>() {
    override fun process(context: WorkerContext<Unit, ValidationResult>): MincerResultType {
        if (context.name.simpleName == "package-info") {
            //if (model.sidesFor(FolderAtom(context.name.name.dropLast(13)))?.containsAny(config.targetSides) == false) {
            //    return MincerResultType.DELETED
            //}
            return MincerResultType.DELETED
        }
        var modified = false
        modified = modified or deleteAnnotation(context.info.node().visibleAnnotations)
        modified = modified or deleteAnnotation(context.info.node().invisibleAnnotations)
        val root = ClassAtom(context.name)
        if (model.sidesFor(root)?.containsAny(config.targetSides) == false)
            return MincerResultType.DELETED

        context.info.node().interfaces?.removeIf { nterface ->
            val shouldBeDeleted =
                model.sidesFor(ClassAtom(ClassTypeReference.fromInternal(nterface)))?.containsAny(config.targetSides) == false

            modified = modified or shouldBeDeleted
            shouldBeDeleted
        }

        context.info.node().fields?.removeIf { field ->
            modified = modified or deleteAnnotation(field.visibleAnnotations)
            modified = modified or deleteAnnotation(field.invisibleAnnotations)
            val shouldBeDeleted =
                model.sidesFor(FieldAtom(context.name, field.name))?.containsAny(config.targetSides) == false
            modified = modified or shouldBeDeleted
            shouldBeDeleted
        }

        context.info.node().methods.removeIf { method ->
            modified = modified or deleteAnnotation(method.visibleAnnotations)
            modified = modified or deleteAnnotation(method.invisibleAnnotations)
            val atom = MethodAtom(context.name, method)
            val shouldBeDeleted = model.sidesFor(atom)?.containsAny(config.targetSides) == false

            if (shouldBeDeleted) {
                modified = true
                return@removeIf true
            }

            val shouldBeCleared = model.sidesFor(MethodBodyAtom(atom))?.containsAny(config.targetSides) == false
            if (shouldBeCleared) {
                modified = true
                cleanMethodBody(method)
            }

            false
        }

        return if (modified) MincerResultType.MODIFIED else MincerResultType.SKIPPED
    }

    private fun deleteAnnotation(annotations: MutableList<AnnotationNode>?) : Boolean {
        annotations ?: return false
        if (!config.removeAnnotations) return false
        return annotations.removeIf { it.desc == config.annotation.desc() }
    }

    private fun cleanMethodBody(method: MethodNode) {
        val returnType = Type.getReturnType(method.desc)!!.toReference()


        method.exceptions = arrayListOf()
        method.maxLocals = 0
        method.maxStack = 0
        method.tryCatchBlocks = arrayListOf()
        method.localVariables = null

        method.instructions = InsnList()
        val appender = SimpleAppender(LocalVariablesSorter(method.access, method.desc, method))

        if (returnType == ClassTypeReference(Void::class)) {
            appender += LoadNullInstruction(returnType)
            appender += ReturnSomethingInstruction(returnType)
            return
        }

        val kind = if (returnType is PrimitiveTypeReference) returnType.kind else null
        if (kind != null) {
            if (kind == PrimitiveKind.VOID) {
                appender += ReturnInstruction
                return
            }
            when (kind) {
                PrimitiveKind.BOOLEAN -> appender += BooleanLoadInstruction(false)
                PrimitiveKind.BYTE -> appender += ByteLoadInstruction(0)
                PrimitiveKind.SHORT -> appender += ShortLoadInstruction(0)
                PrimitiveKind.INT -> appender += IntLoadInstruction(0)
                PrimitiveKind.CHAR -> appender += CharLoadInstruction(0.toChar())
                PrimitiveKind.LONG -> appender += LongLoadInstruction(0)
                PrimitiveKind.FLOAT -> appender += FloatLoadInstruction(0f)
                PrimitiveKind.DOUBLE -> appender += DoubleLoadInstruction(0.0)
                PrimitiveKind.VOID -> {}
            }

            if (returnType is ClassTypeReference) {
                PrimitivesAdapter.wrap(appender, kind)
            }
            appender += ReturnSomethingInstruction(returnType)
        } else {
            appender += LoadNullInstruction(returnType)
            appender += ReturnSomethingInstruction(returnType)
        }
    }
}