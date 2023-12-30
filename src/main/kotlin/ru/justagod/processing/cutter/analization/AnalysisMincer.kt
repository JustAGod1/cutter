package ru.justagod.processing.cutter.analization

import ru.justagod.processing.cutter.base.AugmentedMincer
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.model.ProjectModel

class AnalysisMincer(private val model: ProjectModel, config: CutterConfig) : AugmentedMincer<Unit, Unit> (
    listOf(
        LambdaAnalysisAugment(model, config),
        MembersMarkerAugment(model, config),
        InnerClassesMarkerAugment(model, config),
        InheritanceMarkerAugment(model, config),
        InnerClassesMarkerAugment(model, config),
        KotlinAnalysisAugment(model, config)
    )
) {

    override fun endProcessing(input: Unit, output: Unit) {
        model.finish()
    }
}