package ru.justagod.mincer.filter

import ru.justagod.mincer.Mincer
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory


interface ClassFilter {

    fun isValid(name: ClassTypeReference, mincer: Mincer): Boolean

}