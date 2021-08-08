package ru.justagod.cutter.mincer.filter

import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.model.ClassTypeReference

object WalkThroughFilter : ClassFilter {
    override fun isValid(name: ClassTypeReference, mincer: Mincer): Boolean = true
}