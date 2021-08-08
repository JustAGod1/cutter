package ru.justagod.processing.cutter.config

import ru.justagod.model.ClassTypeReference

class InvokeClass(
    val name: ClassTypeReference,
    val sides: Set<SideName>,
    val functionalMethod: MethodDesc
) {

    override fun toString(): String = "${name.name}.$functionalMethod $sides"

}