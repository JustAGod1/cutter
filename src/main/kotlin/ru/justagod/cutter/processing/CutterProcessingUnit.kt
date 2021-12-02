package ru.justagod.cutter.processing

import ru.justagod.cutter.mincer.pipeline.MincerPipeline
import ru.justagod.cutter.mincer.pipeline.MincerPipelineController
import ru.justagod.cutter.processing.analization.AnalysisMincer
import ru.justagod.cutter.processing.config.CutterConfig
import ru.justagod.cutter.processing.config.SideName
import ru.justagod.cutter.processing.model.ProjectModel
import ru.justagod.cutter.processing.transformation.TransformationMincer
import ru.justagod.cutter.processing.transformation.validation.ValidationError
import ru.justagod.cutter.processing.transformation.validation.ValidationResult

object CutterProcessingUnit {

    fun makePipeline(input: CutterConfig, model: ProjectModel): MincerPipelineController<ValidationResult> {
        return MincerPipeline
            .make(AnalysisMincer(model, input))
            .join(TransformationMincer(model, input), ValidationResult(emptyList()))
            .build()

    }

    fun makePipeline(input: CutterConfig): MincerPipelineController<ValidationResult> {
        return makePipeline(input, ProjectModel(input.primalSides))
    }

    val server = SideName.make("server")
    val client = SideName.make("client")

    fun reportValidationResults(
        errorsBySide: Map<Collection<SideName>, List<ValidationError>>
    ): String = buildString {
        if (errorsBySide.isNotEmpty()) {
            val totalNumErrors = errorsBySide.map { it.value.size }.sum()
            append("\n")
            append("Validation failed. Total number of validation errors: $totalNumErrors\n")
            for ((side, errors) in errorsBySide) {
                append("\n")
                append("${side.joinToString()} validated with ${errors.size} errors: \n")
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