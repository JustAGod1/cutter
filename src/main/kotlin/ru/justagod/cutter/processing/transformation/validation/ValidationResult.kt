package ru.justagod.cutter.processing.cutter.transformation.validation

class ValidationResult(val errors: List<ValidationError>) {

    override fun toString(): String {
        return errors.joinToString(separator = "\n")
    }

}