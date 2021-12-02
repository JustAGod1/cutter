package ru.justagod.processing.cutter.transformation.validation

import ru.justagod.cutter.processing.transformation.validation.ValidationError

class ValidationResult(val errors: List<ValidationError>) {

    override fun toString(): String {
        return errors.joinToString(separator = "\n")
    }

}