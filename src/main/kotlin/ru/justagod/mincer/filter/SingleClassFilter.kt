package ru.justagod.mincer.filter

import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory

class SingleClassFilter(private val target: ClassTypeReference) : ClassFilter {
    override fun isValid(name: ClassTypeReference, model: () -> ClassModel, inheritance: InheritanceHelper, factory: ModelFactory): Boolean {
        return name == target
    }
}