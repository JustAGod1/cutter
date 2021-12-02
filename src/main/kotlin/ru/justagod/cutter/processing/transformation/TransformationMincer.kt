package ru.justagod.cutter.processing.transformation

import ru.justagod.cutter.processing.base.AugmentedMincer
import ru.justagod.cutter.processing.config.CutterConfig
import ru.justagod.cutter.processing.model.ProjectModel
import ru.justagod.processing.cutter.transformation.validation.ValidationAugment
import ru.justagod.processing.cutter.transformation.validation.ValidationResult

class TransformationMincer(model: ProjectModel, config: CutterConfig) : AugmentedMincer<Unit, ValidationResult>() {

    init {
        register(MembersDeletionAugment(config, model))
        if (config.deleteAnnotations) register(AnnotationRemoverAugment(config.annotation))
    }

    private val validator = ValidationAugment(config, model).register()

    override fun endProcessing(input: Unit, output: ValidationResult): ValidationResult {
        return ValidationResult(validator.result)
    }

}