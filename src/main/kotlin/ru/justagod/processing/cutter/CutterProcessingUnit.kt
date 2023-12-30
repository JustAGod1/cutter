package ru.justagod.processing.cutter

import ru.justagod.mincer.pipeline.MincerPipeline
import ru.justagod.mincer.pipeline.MincerPipelineController
import ru.justagod.model.ClassTypeReference
import ru.justagod.processing.aggregation.ProcessingUnit
import ru.justagod.processing.cutter.analization.AnalysisMincer
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.config.InvokeClass
import ru.justagod.processing.cutter.config.MethodDesc
import ru.justagod.processing.cutter.config.SideName
import ru.justagod.processing.cutter.model.ProjectModel
import ru.justagod.processing.cutter.transformation.TransformationMincer
import ru.justagod.processing.cutter.transformation.validation.ValidationError
import ru.justagod.processing.cutter.transformation.validation.ValidationResult

object CutterProcessingUnit : ProcessingUnit<CutterConfig>() {

    fun makePipeline(input: CutterConfig, model: ProjectModel = ProjectModel(input.primalSides)): MincerPipelineController<ValidationResult> {
        return MincerPipeline
            .make(AnalysisMincer(model, input))
            .join(TransformationMincer(model, input), ValidationResult(emptyList()))
            .build()

    }

    override fun makePipelines(input: CutterConfig): List<MincerPipelineController<*>> {
        return listOf(makePipeline(input, ProjectModel(input.primalSides)))
    }

    val server = SideName.make("server")
    val client = SideName.make("client")
    val sides = listOf(server, client)


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