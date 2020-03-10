package ru.justagod.plugin.processing

import ru.justagod.mincer.filter.WalkThroughFilter
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.util.join
import ru.justagod.mincer.util.makeFirstSimple
import ru.justagod.plugin.data.BakedCutterTaskData
import ru.justagod.plugin.processing.model.ProjectModel
import ru.justagod.plugin.processing.pipeline.*
import ru.justagod.plugin.processing.pipeline.validation.ValidationMincer
import ru.justagod.plugin.processing.pipeline.validation.ValidationResult

object CutterPipelines {

    fun makePipelineWithValidation(data: BakedCutterTaskData): Pipeline<*, ValidationResult> {
        return makePipeline(data)
                .join(
                        ValidationMincer(data.primalSides, data.validationOverrideAnnotation?.name),
                        WalkThroughFilter,
                        null
                )
    }

    fun makePipeline(data: BakedCutterTaskData): Pipeline<*, ProjectModel> {
        return Pipeline
                .makeFirstSimple(
                        FirstAnalyzerMincer(data.annotation.name),
                        WalkThroughFilter,
                        ProjectModel(data.invocators)
                )
                .join(
                        SecondAnalyzerMincer(data.primalSides.toSet()),
                        WalkThroughFilter,
                        null
                )
                .join(
                        ThirdAnalyzerMincer(data.primalSides.toSet()),
                        WalkThroughFilter,
                        null
                )
                .join(
                        FourthAnalyzerMincer(data.primalSides.toSet()),
                        WalkThroughFilter,
                        null
                )
                .let {
                    if (data.removeAnnotations) it
                            .join(
                                    AnnotationsRemoverMincer(data.annotation.name),
                                    WalkThroughFilter,
                                    null
                            )
                    else it
                }
                .join(
                        CutterMincer(data.targetSides, data.primalSides.toSet()),
                        WalkThroughFilter,
                        null
                )

    }

}