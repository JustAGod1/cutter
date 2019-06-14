package ru.justagod.mincer.filter

import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory


class AnnotationFilter(private val annotation: ClassTypeReference) : ClassFilter {
    override fun isValid(name: ClassTypeReference, model: () -> ClassModel, inheritance: InheritanceHelper, factory: ModelFactory): Boolean {
        inheritance.walk(name) {
            if (it.invisibleAnnotations.any { it.key == annotation }
                    || it.visibleAnnotations.any { it.key == annotation }) {
                return true
            }
        }
        return false
    }
}