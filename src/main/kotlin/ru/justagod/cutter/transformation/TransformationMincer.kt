package ru.justagod.processing.cutter.transformation

import ru.justagod.mincer.processor.SubMincer
import ru.justagod.processing.cutter.base.AugmentedMincer
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.model.ProjectModel
import ru.justagod.processing.cutter.transformation.validation.ValidationAugment
import ru.justagod.processing.cutter.transformation.validation.ValidationError
import ru.justagod.processing.cutter.transformation.validation.ValidationResult

class TransformationMincer(model: ProjectModel, config: CutterConfig) : AugmentedMincer<Unit, ValidationResult>() {

    init {
        register(MembersDeletionAugment(config, model))
    }

    private val validator = ValidationAugment(config, model).register()

    override fun endProcessing(input: Unit, output: ValidationResult): ValidationResult {
        return ValidationResult(validator.result)
    }

}