package ru.justagod.mincer.filter

import ru.justagod.mincer.Mincer
import ru.justagod.model.ClassTypeReference

class InheritanceFilter(private val parent: ClassTypeReference) : ClassFilter {
    override fun isValid(name: ClassTypeReference, mincer: Mincer) =
            mincer.inheritance.isChild(name, parent)
}