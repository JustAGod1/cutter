package ru.justagod.cutter.model.factory

import ru.justagod.cutter.model.AbstractModel
import ru.justagod.cutter.model.ClassModel
import ru.justagod.cutter.model.ClassTypeReference

interface ModelFactory {

    fun makeModel(type: ClassTypeReference, parent: AbstractModel?): ClassModel
}