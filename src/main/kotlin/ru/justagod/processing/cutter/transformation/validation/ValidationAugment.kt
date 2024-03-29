package ru.justagod.processing.cutter.transformation.validation

import org.objectweb.asm.Handle
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.*
import ru.justagod.model.factory.BytecodeModelFactory
import ru.justagod.processing.cutter.base.MincerAugment
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.config.SideName
import ru.justagod.processing.cutter.model.*
import ru.justagod.utils.containsAny
import ru.justagod.utils.nope

class ValidationAugment(private val config: CutterConfig, private val model: ProjectModel) :
    MincerAugment<Unit, ValidationResult>() {

    val result = arrayListOf<ValidationError>()

    override fun process(context: WorkerContext<Unit, ValidationResult>): MincerResultType {
        if (shouldNotValidate(context.info.node())) return MincerResultType.SKIPPED
        validateParents(context, context.info.node())

        context.info.node().fields?.forEach { field ->
            if (!shouldNotValidate(field)) {
                validateType(
                    context,
                    fetchTypeReference(field.desc),
                    FieldLocation(context.name, context.info.node().sourceFile, field.name)
                )
            }
        }

        for (method in context.info.node().methods) {
            if (shouldNotValidate(method)) continue
            validateMethodDesc(context, context.info.node(), method)
            validateMethodBody(context, context.info.node(), method)
        }

        return MincerResultType.SKIPPED
    }


    private fun validateParents(context: WorkerContext<Unit, ValidationResult>, node: ClassNode) {
        val location = ClassDescLocation(ClassTypeReference.fromInternal(node.name), node.sourceFile)

        validateClassRef(context, ClassTypeReference.fromInternal(node.superName), location)
        node.interfaces?.forEach { validateClassRef(context, ClassTypeReference(it), location) }
    }

    private fun validateMethodBody(context: WorkerContext<Unit, ValidationResult>, owner: ClassNode, method: MethodNode) {
        var location = 0
        val ownerName = ClassTypeReference.fromInternal(owner.name)
        fun location() = MethodBodyLocation(ownerName, owner.sourceFile, method.name, location)
        for (insn in method.instructions) {
            when (insn) {
                is LineNumberNode -> location = insn.line

                is TypeInsnNode -> validateType(context, fetchObscureTypeReference(insn.desc), location())

                is MethodInsnNode -> validateMethodRef(
                    context,
                    fetchObscureTypeReference(insn.owner) as? ClassTypeReference ?: continue,
                    insn.name, insn.desc, location()
                )

                is FieldInsnNode -> validateFieldRef(
                    context,
                    ClassTypeReference.fromInternal(insn.owner), insn.name, location()
                )

                is MultiANewArrayInsnNode -> validateType(
                    context,
                    fetchTypeReference(insn.desc), location()
                )

                is InvokeDynamicInsnNode -> {
                    if (insn.bsm.name != "metafactory" && insn.bsm.name != "altMetafactory") continue
                    val implHandle = insn.bsmArgs[1] as Handle
                    val owner = ClassTypeReference.fromInternal(implHandle.owner)
                    val name = implHandle.name
                    val desc = implHandle.desc
                    validateMethodRef(context, owner, name, desc, location())
                }
            }
        }
    }

    private fun fetchObscureTypeReference(descOrInternal: String): TypeReference {
        if (descOrInternal.startsWith("[")) {
            return fetchTypeReference(descOrInternal)
        } else {
            return ClassTypeReference.fromInternal(descOrInternal)
        }
    }

    private fun validateMethodDesc(context: WorkerContext<Unit, ValidationResult>, owner: ClassNode, method: MethodNode) {
        val location = MethodDescLocation(
            ClassTypeReference.fromInternal(owner.name),
            owner.sourceFile,
            method.name,
            tryToFindLine(method)
        )

        // One day somebody will reincarnate this code
        /*
        val note = if (model.isLambda(MethodAtom(ClassTypeReference.fromInternal(owner.name), method)))
            "Probably your lambda captured some arguments with wrong types"
        else null
         */
        val note = null

        for (argumentType in Type.getArgumentTypes(method.desc)) {
            validateType(context, argumentType.toReference(), location, note)
        }

        validateType(context, Type.getReturnType(method.desc).toReference(), location, note)
    }

    private fun tryToFindLine(method: MethodNode): Int {
        for (instruction in method.instructions ?: return 0) {
            if (instruction is LineNumberNode) return instruction.line
        }
        return 0
    }

    private fun validateType(context: WorkerContext<Unit, ValidationResult>, type: TypeReference, location: Location, note: String? = null) {
        if (type is ClassTypeReference) validateClassRef(context, type, location, note)
        else if (type is ArrayTypeReference) validateType(context, type.arrayType, location, note)
    }

    private fun validateMethodRef(context: WorkerContext<Unit, ValidationResult>, owner: ClassTypeReference, name: String, desc: String, location: Location, note: String? = null) {
        val hisSides = searchThroughParents(context, owner) { MethodAtom(it, name, desc) } ?: return validateClassRef(context, owner, location, note)
        if (!hisSides.containsAny(config.targetSides)) {
            addValidationError(MethodNotFoundValidationError(owner, name, desc, location, hisSides, note))
        }
    }

    private fun validateFieldRef(context: WorkerContext<Unit, ValidationResult>, owner: ClassTypeReference, name: String, location: Location, note: String? = null) {
        val hisSides = searchThroughParents(context, owner) { FieldAtom(it, name) } ?: return validateClassRef(context, owner, location, note)
        if (!hisSides.containsAny(config.targetSides)) {
            addValidationError(FieldNotFoundValidationError(owner, name, location, hisSides, note))
        }
    }

    private fun validateClassRef(context: WorkerContext<Unit, ValidationResult>, ref: ClassTypeReference, location: Location, note: String? = null) {
        val hisSides = model.sidesFor(ClassAtom(ref))
        if (hisSides == null) {
            try {
                context.mincer.makeNode(ref)
            } catch (e: BytecodeModelFactory.BytecodeNotFoundException) {
                addValidationError(ClassNotFoundValidationError(ref, location, emptySet(), note))
            }
        } else if (!hisSides.containsAny(config.targetSides)) {
            addValidationError(ClassNotFoundValidationError(ref, location, hisSides, note))
        }
    }

    private fun searchThroughParents(context: WorkerContext<Unit, ValidationResult>, child: ClassTypeReference, block: (ClassTypeReference) -> ProjectAtom): Set<SideName>? {
        try {
            context.mincer.inheritance.walk(child) {
                if (it.access.isInterface) return@walk
                val atom = block(it.name)
                val sides = model.sidesFor(atom)
                if (sides != null) return sides
            }
        } catch (e: BytecodeModelFactory.BytecodeNotFoundException) {
            println("Could not resolve hierarchy for $child (at type ${e.className}), this should lead to a validation error...")
        }

        return null
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