package ru.justagod.cutter.processing.cutter.analization

import ru.justagod.cutter.processing.cutter.base.AugmentedMincer
import ru.justagod.cutter.processing.cutter.config.CutterConfig
import ru.justagod.cutter.processing.cutter.model.ProjectModel

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