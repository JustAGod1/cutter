package ru.justagod.mincer.filter

import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory

class DoubleFilter(private val first: ClassFilter, private val second: ClassFilter) : ClassFilter {
    override fun isValid(name: ClassTypeReference, model: () -> ClassModel, inheritance: InheritanceHelper, factory: ModelFactory): Boolean {
        return first.isValid(name, model, inheritance, factory) && second.isValid(name, model, inheritance, factory)
    }
}