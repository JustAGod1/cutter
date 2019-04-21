package ru.justagod.model.factory

import ru.justagod.model.AbstractModel
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference

interface ModelFactory {

    fun makeModel(type: ClassTypeReference, parent: AbstractModel?): ClassModel
}