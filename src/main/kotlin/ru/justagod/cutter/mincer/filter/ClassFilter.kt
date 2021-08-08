package ru.justagod.cutter.mincer.filter

import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.model.ClassTypeReference


interface ClassFilter {

    fun isValid(name: ClassTypeReference, mincer: Mincer): Boolean

}