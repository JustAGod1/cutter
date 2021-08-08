package ru.justagod.cutter.processing.cutter.transformation.validation

import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.processor.WorkerContext
import ru.justagod.cutter.model.*
import ru.justagod.cutter.processing.cutter.base.MincerAugment
import ru.justagod.cutter.processing.cutter.config.CutterConfig
import ru.justagod.cutter.processing.cutter.model.ClassAtom
import ru.justagod.cutter.processing.cutter.model.FieldAtom
import ru.justagod.cutter.processing.cutter.model.MethodAtom
import ru.justagod.cutter.processing.cutter.model.ProjectModel
import ru.justagod.cutter.utils.containsAny

class ValidationAugment(private val config: CutterConfig, private val model: ProjectModel) :
    MincerAugment<Unit, ValidationResult>() {

    val result = arrayListOf<ValidationError>()

    override fun process(context: WorkerContext<Unit, ValidationResult>): MincerResultType {
        if (shouldNotValidate(context.info.node())) return MincerResultType.SKIPPED
        validateParents(context.info.node())

        context.info.node().fields?.forEach { field ->
            if (!shouldNotValidate(field))
                validateType(
                    fetchTypeReference(field.desc),
                    FieldLocation(context.name, context.info.node().sourceFile, field.name)
                )
        }

        for (method in context.info.node().methods) {
            if (shouldNotValidate(method)) continue
            validateMethodDesc(context.info.node(), method)
            validateMethodBody(context.info.node(), method)
        }

        return MincerResultType.SKIPPED
    }


    private fun validateParents(node: ClassNode) {
        val location = ClassDescLocation(ClassTypeReference.fromInternal(node.name), node.sourceFile)

        validateClassRef(ClassTypeReference.fromInternal(node.superName), location)
        node.interfaces?.forEach { validateClassRef(ClassTypeReference(it), location) }
    }

    private fun validateMethodBody(owner: ClassNode, method: MethodNode) {
        var location = 0
        val ownerName = ClassTypeReference.fromInternal(owner.name)
        fun location() = MethodBodyLocation(ownerName, owner.sourceFile, method.name, location)
        for (insn in method.instructions) {
            when (insn) {
                is LineNumberNode -> location = insn.line

                is TypeInsnNode -> validateClassRef(ClassTypeReference.fromInternal(insn.desc), location())

                is MethodInsnNode -> validateMethodRef(
                    ClassTypeReference.fromInternal(insn.owner), insn.name, insn.desc, location()
                )

                is FieldInsnNode -> validateFieldRef(
                    ClassTypeReference.fromInternal(insn.owner), insn.name, location()
                )

                is MultiANewArrayInsnNode -> validateType(
                    fetchTypeReference(insn.desc), location()
                )
            }
        }
    }

    private fun validateMethodDesc(owner: ClassNode, method: MethodNode) {
        val location = MethodDescLocation(
            ClassTypeReference.fromInternal(owner.name),
            owner.sourceFile,
            method.name,
            tryToFindLine(method)
        )

        val note = if (model.isLambda(MethodAtom(ClassTypeReference.fromInternal(owner.name), method)))
            "Probably your lambda captured some arguments with wrong types"
        else null

        for (argumentType in Type.getArgumentTypes(method.desc)) {
            validateType(argumentType.toReference(), location, note)
        }

        validateType(Type.getReturnType(method.desc).toReference(), location, note)
    }

    private fun tryToFindLine(method: MethodNode): Int {
        for (instruction in method.instructions ?: return 0) {
            if (instruction is LineNumberNode) return instruction.line
        }
        return 0
    }

    private fun validateType(type: TypeReference, location: Location, note: String? = null) {
        if (type is ClassTypeReference) validateClassRef(type, location, note)
        else if (type is ArrayTypeReference) validateType(type.arrayType, location, note)
    }

    private fun validateMethodRef(owner: ClassTypeReference, name: String, desc: String, location: Location, note: String? = null) {
        val hisSides = model.sidesFor(MethodAtom(owner, name, desc)) ?: return
        if (!hisSides.containsAny(config.targetSides)) {
            addValidationError(MethodNotFoundValidationError(owner, name, desc, location, hisSides, note))
        }
    }

    private fun validateFieldRef(owner: ClassTypeReference, name: String, location: Location, note: String? = null) {
        val hisSides = model.sidesFor(FieldAtom(owner, name)) ?: return
        if (!hisSides.containsAny(config.targetSides)) {
            addValidationError(FieldNotFoundValidationError(owner, name, location, hisSides, note))
        }
    }

    private fun validateClassRef(ref: ClassTypeReference, location: Location, note: String? = null) {
        val hisSides = model.sidesFor(ClassAtom(ref)) ?: return
        if (!hisSides.containsAny(config.targetSides)) {
            addValidationError(ClassNotFoundValidationError(ref, location, hisSides, note))
        }
    }

    @Synchronized
    private fun addValidationError(error: ValidationError) {
        result += error
    }

    private fun shouldNotValidate(node: ClassNode): Boolean {
        return hasNoValidationAnnotation(node.visibleAnnotations) || hasNoValidationAnnotation(node.invisibleAnnotations)
    }

    private fun shouldNotValidate(node: FieldNode): Boolean {
        return hasNoValidationAnnotation(node.visibleAnnotations) || hasNoValidationAnnotation(node.invisibleAnnotations)
    }

    private fun shouldNotValidate(node: MethodNode): Boolean {
        return hasNoValidationAnnotation(node.visibleAnnotations) || hasNoValidationAnnotation(node.invisibleAnnotations)
    }

    private fun hasNoValidationAnnotation(annotations: List<AnnotationNode>?): Boolean {
        annotations ?: return false

        return annotations.any { ClassTypeReference.fromDesc(it.desc) == config.validationOverrideAnnotation }
    }

}