package ru.justagod.plugin.processing

import ru.justagod.mincer.filter.WalkThroughFilter
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.util.join
import ru.justagod.mincer.util.makeFirstSimple
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.processing.model.ProjectModel
import ru.justagod.plugin.processing.pipeline.*

object CutterPipelines {


    fun makePipeline(annotation: String, data: CutterTaskData): Pipeline<*, Unit> {
        return Pipeline
                .makeFirstSimple(
                        FirstAnalyzerMincer(annotation, data.primalSides),
                        WalkThroughFilter,
                        ProjectModel(data.invokeClasses)
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
                .join(
                        CutterMincer(data.targetSides, data.primalSides.toSet()),
                        WalkThroughFilter,
                        Unit
                )

    }

}