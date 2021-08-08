package ru.justagod.cutter.processing.cutter

import ru.justagod.cutter.mincer.pipeline.MincerPipeline
import ru.justagod.cutter.mincer.pipeline.MincerPipelineController
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.processing.cutter.analization.AnalysisMincer
import ru.justagod.cutter.processing.cutter.config.CutterConfig
import ru.justagod.cutter.processing.cutter.config.InvokeClass
import ru.justagod.cutter.processing.cutter.config.MethodDesc
import ru.justagod.cutter.processing.cutter.config.SideName
import ru.justagod.cutter.processing.cutter.model.ProjectModel
import ru.justagod.cutter.processing.cutter.transformation.TransformationMincer
import ru.justagod.cutter.processing.cutter.transformation.validation.ValidationError
import ru.justagod.cutter.processing.cutter.transformation.validation.ValidationResult

object CutterProcessingUnit {

    fun makePipeline(input: CutterConfig, model: ProjectModel): MincerPipelineController<ValidationResult> {
        return MincerPipeline
            .make(AnalysisMincer(model, input))
            .join(TransformationMincer(model, input), ValidationResult(emptyList()))
            .build()

    }

    fun makePipelines(input: CutterConfig): List<MincerPipelineController<*>> {
        return listOf(makePipeline(input, ProjectModel(input.primalSides)))
    }

    val backend = SideName.make("backend")
    val frontend = SideName.make("frontend")
    val client = SideName.make("client")
    val sides = listOf(backend, frontend, client)
    fun defaultConfig(targetSides: Set<SideName>) = CutterConfig(
        annotation = ClassTypeReference("gloomyfolken.bundle.common.core.BundleSideOnly"),
        validationOverrideAnnotation = ClassTypeReference("gloomyfolken.bundle.common.core.NoValidation"),
        removeAnnotations = false,
        primalSides = setOf(frontend, client),
        targetSides = targetSides,
        invocators = listOf(
            InvokeClass(
                name = ClassTypeReference("gloomyfolken.bundle.common.core.InvokeSideOnly\$InvokeBackendOnly"),
                sides = setOf(backend),
                functionalMethod = MethodDesc("run", "()V")
            ),
            InvokeClass(
                name = ClassTypeReference("gloomyfolken.bundle.common.core.InvokeSideOnly\$InvokeFrontendOnly"),
                sides = setOf(frontend),
                functionalMethod = MethodDesc("run", "()V")
            ),
            InvokeClass(
                name = ClassTypeReference("gloomyfolken.bundle.common.core.InvokeSideOnly\$InvokeClientOnly"),
                sides = setOf(client),
                functionalMethod = MethodDesc("run", "()V")
            ),
            InvokeClass(
                name = ClassTypeReference("gloomyfolken.bundle.common.core.InvokeWithResult\$InvokeBackendOnly"),
                sides = setOf(backend),
                functionalMethod = MethodDesc("run", "()Ljava/lang/Object;")
            ),
            InvokeClass(
                name = ClassTypeReference("gloomyfolken.bundle.common.core.InvokeWithResult\$InvokeFrontendOnly"),
                sides = setOf(frontend),
                functionalMethod = MethodDesc("run", "()Ljava/lang/Object;")
            ),
            InvokeClass(
                name = ClassTypeReference("gloomyfolken.bundle.common.core.InvokeWithResult\$InvokeClientOnly"),
                sides = setOf(client),
                functionalMethod = MethodDesc("run", "()Ljava/lang/Object;")
            )
        ),
        markers = listOf()
    )


    fun reportValidationResults(
        errorsBySide: Map<SideName, List<ValidationError>>
    ): String = buildString {
        if (errorsBySide.isNotEmpty()) {
            val totalNumErrors = errorsBySide.map { it.value.size }.sum()
            append("\n")
            append("Validation failed. Total number of validation errors: $totalNumErrors\n")
            for ((side, errors) in errorsBySide) {
                append("\n")
                append("$side validated with ${errors.size} errors: \n")
                errors.forEach { append(it).append("\n") }
            }
            append("Summary:\n")
            for ((side, errors) in errorsBySide) {
                append("\n")
                append("$side:\n")
                errors.groupBy { it.location.clazz.name }.toList().map { it.first to it.second.size }
                    .sortedByDescending { it.second }
                    .forEach {
                        append("${it.first}: ${it.second}\n")
                    }
            }
        }
    }
}