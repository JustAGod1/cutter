package ru.justagod.mincer.filter

import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory

class OrFilter(private vararg val filters: ClassFilter) : ClassFilter {
    override fun isValid(name: ClassTypeReference, model: () -> ClassModel, inheritance: InheritanceHelper, factory: ModelFactory): Boolean {
        for (filter in filters) {
            if (filter.isValid(name, model, inheritance, factory)) return true
        }
        return false
    }

}