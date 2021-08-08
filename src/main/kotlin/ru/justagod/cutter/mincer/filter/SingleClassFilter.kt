package ru.justagod.cutter.mincer.filter

import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.model.ClassTypeReference

class SingleClassFilter(val target: ClassTypeReference) : ClassFilter {
    override fun isValid(name: ClassTypeReference, mincer: Mincer): Boolean {
        return name == target
    }
}