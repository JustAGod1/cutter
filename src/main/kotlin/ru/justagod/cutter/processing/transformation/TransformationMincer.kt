package ru.justagod.cutter.processing.cutter.transformation

import ru.justagod.cutter.processing.cutter.base.AugmentedMincer
import ru.justagod.cutter.processing.cutter.config.CutterConfig
import ru.justagod.cutter.processing.cutter.model.ProjectModel
import ru.justagod.cutter.processing.cutter.transformation.validation.ValidationAugment
import ru.justagod.cutter.processing.cutter.transformation.validation.ValidationResult

class TransformationMincer(model: ProjectModel, config: CutterConfig) : AugmentedMincer<Unit, ValidationResult>() {

    init {
        register(MembersDeletionAugment(config, model))
    }

    private val validator = ValidationAugment(config, model).register()

    override fun endProcessing(input: Unit, output: ValidationResult): ValidationResult {
        return ValidationResult(validator.result)
    }

}