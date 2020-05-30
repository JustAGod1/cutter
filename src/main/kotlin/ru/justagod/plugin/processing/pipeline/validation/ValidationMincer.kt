package ru.justagod.plugin.processing.pipeline.validation

import org.objectweb.asm.Handle
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.*
import ru.justagod.plugin.data.DynSideMarker
import ru.justagod.plugin.data.DynSideMarkerBuilder
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.model.MethodDesc
import ru.justagod.plugin.processing.model.PathHelper
import ru.justagod.plugin.processing.model.ProjectModel
import ru.justagod.plugin.processing.pipeline.SidlyInstructionsIter
import ru.justagod.plugin.processing.pipeline.validation.data.ClassError
import ru.justagod.plugin.processing.pipeline.validation.data.FieldError
import ru.justagod.plugin.processing.pipeline.validation.data.MethodError
import ru.justagod.plugin.processing.pipeline.validation.data.ValidationError

typealias ValidationResult = Map<SideName, List<ValidationError>>

// Probably we also have to check for annotations validity but thanks to java runtime existence of
// annotations at runtime is quite optional.
class ValidationMincer(
        private val primalSides: Set<SideName>,
        annotation: String?,
        private val markers: List<DynSideMarker>
) : SubMincer<ProjectModel, ValidationResult> {

    private val annotationDesc = annotation?.let { "L${it.replace('.', '/')};" }
    private val result = hashMapOf<SideName, MutableList<ValidationError>>()

    override fun process(context: WorkerContext<ProjectModel, ValidationResult>): MincerResultType {
        val node = context.info!!.node
        if (validationDisabled(node.invisibleAnnotations)) return MincerResultType.SKIPPED
        if (validationDisabled(node.visibleAnnotations)) return MincerResultType.SKIPPED

        node.fields?.forEach {
            if (validationDisabled(it.invisibleAnnotations)) return@forEach
            if (validationDisabled(it.visibleAnnotations)) return@forEach
            val fieldType = fetchTypeReference(it.desc).unpack()
            if (fieldType !is ClassTypeReference) return@forEach
            val sidesOfExistence = context.input.sidesTree.get(
                    PathHelper.field(context.name, it.name, it.desc),
                    primalSides
            )

            considerClass(sidesOfExistence, context.name, it.name, fieldType, context.input, node.sourceFile, 0)
        }

        node.methods?.forEach {
            if (validationDisabled(it.invisibleAnnotations)) return@forEach
            if (validationDisabled(it.visibleAnnotations)) return@forEach
            val sidesOfExistence = context.input.sidesTree.get(
                    PathHelper.method(context.name, it.name, it.desc),
                    primalSides
            )
            val args = Type.getArgumentTypes(it.desc)
            for (arg in args) {
                val type = fetchTypeReference(arg).unpack()
                if (type !is ClassTypeReference) continue
                considerClass(sidesOfExistence, context.name, it.name, type, context.input, node.sourceFile, 0)
            }
            val returnType = fetchTypeReference(Type.getReturnType(it.desc)).unpack()
            if (returnType is ClassTypeReference) {
                considerClass(sidesOfExistence, context.name, it.name, returnType, context.input, node.sourceFile, 0)
            }
            analyzeMethodBody(context.name, it, sidesOfExistence, context.input, node.sourceFile)
        }

        return MincerResultType.SKIPPED;
    }

    private fun validationDisabled(annotations: List<AnnotationNode>?): Boolean {
        if (annotationDesc == null) return false
        if (annotations == null) return false
        return annotations.any { it.desc == annotationDesc }
    }

    private fun considerClass(
            sidesOfExistence: Set<SideName>,
            holder: ClassTypeReference,
            name: String,
            klass: ClassTypeReference,
            project: ProjectModel,
            src: String?,
            line: Int
    ) {
        val typeSides = project.sidesTree.get(
                PathHelper.klass(klass), primalSides
        )
        analyze(sidesOfExistence, typeSides) {
            inscribeError(it, ClassError(
                    klass,
                    holder,
                    name,
                    src,
                    line
            ))
        }

    }

    private fun analyze(sidesOfExistence: Set<SideName>, sidesLimitation: Set<SideName>, block: (SideName) -> Unit) {
        for (sideName in sidesOfExistence) {
            if (sideName !in sidesLimitation) block(sideName)
        }

    }

    private fun inscribeError(side: SideName, error: ValidationError) {
        result.computeIfAbsent(side) { arrayListOf() } += error
    }

    private fun TypeReference.unpack() = if (this is ArrayTypeReference)
        getArrayTypeRecursively(this) else this

    private fun getArrayTypeRecursively(type: ArrayTypeReference): TypeReference {
        var t = type.arrayType
        while (t is ArrayTypeReference) t = t.arrayType
        return t
    }



    private fun analyzeMethodBody(
            holder: ClassTypeReference,
            method: MethodNode,
            sidesOfExistence: Set<SideName>,
            project: ProjectModel,
            src: String?
    ) {
        var line: Int = 0
        SidlyInstructionsIter.iterate(
                method.instructions,
                sidesOfExistence,
                markers
        ) { (instruction, sides) ->
                if (instruction is MethodInsnNode) {
                    considerMethodRef(
                            instruction.owner,
                            instruction.name,
                            instruction.desc,
                            sides,
                            project,
                            holder,
                            method,
                            src,
                            line
                    )
                } else if (instruction is FieldInsnNode) {
                    val fieldHolder = ClassTypeReference.fromInternal(instruction.owner)
                    analyze(
                            sides,
                            project.sidesTree.get(
                                    PathHelper.method(
                                            fieldHolder,
                                            instruction.name, instruction.desc
                                    ),
                                    primalSides
                            )
                    ) {
                        inscribeError(it, FieldError(
                                fieldHolder, instruction.name, holder, method.name, src, line
                        ))
                    }
                } else if (instruction is MultiANewArrayInsnNode) {
                    val type = fetchTypeReference(instruction.desc).unpack()
                    if (type is ClassTypeReference) {
                        considerClass(
                                sides,
                                holder,
                                method.name,
                                type,
                                project, src, line
                        )
                    }
                } else if (instruction is TypeInsnNode) {
                    val klass = ClassTypeReference.fromInternal(instruction.desc)
                    considerClass(
                            sides,
                            holder,
                            method.name,
                            klass,
                            project, src, line
                    )
                } else if (instruction is InvokeDynamicInsnNode) {
                    // Actually I'm one hundred percents sure that nobody will ever write their own bootstrap methods
                    considerMethodRef(
                            instruction.bsm.owner, instruction.bsm.name, instruction.bsm.desc,
                            sides, project, holder, method, src, line
                    )

                    val implHandle = instruction.bsmArgs[1] as Handle
                    // We won't ever delete methods that implement bodies of lambdas that will be passed to invokators
                    if (ClassTypeReference.fromInternal(implHandle.owner) != holder
                            || project.lambdaMethods[holder]?.contains(MethodDesc(implHandle.name, implHandle.desc)) != true) {
                        considerMethodRef(
                                implHandle.owner, implHandle.name, implHandle.desc,
                                sides, project, holder, method, src, line
                        )
                    }
                } else if (instruction is LineNumberNode) {
                    line = instruction.line
                }
        }
    }

    private fun considerMethodRef(
            owner: String,
            name: String,
            desc: String,
            sidesOfExistence: Set<SideName>,
            project: ProjectModel,
            holder: ClassTypeReference,
            method: MethodNode,
            src: String?,
            line: Int
    ) {
        val methodHolder = ClassTypeReference.fromInternal(owner)
        analyze(
                sidesOfExistence,
                project.sidesTree.get(
                        PathHelper.method(
                                methodHolder,
                                name, desc
                        ),
                        primalSides
                )
        ) {
            inscribeError(it, MethodError(
                    methodHolder, name, desc, holder, method.name, src, line
            ))
        }
    }

    override fun endProcessing(input: ProjectModel, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<ProjectModel, ValidationResult>) {
        pipeline.value = result
    }
}