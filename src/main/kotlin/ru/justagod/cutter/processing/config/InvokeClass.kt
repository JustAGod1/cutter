package ru.justagod.cutter.processing.config

import ru.justagod.cutter.model.ClassTypeReference
import java.io.Serializable

data class InvokeClass(
    val name: ClassTypeReference,
    val sides: Set<SideName>,
    val functionalMethod: MethodDesc
) : Serializable {

    override fun toString(): String = "${name.name}.$functionalMethod $sides"

}