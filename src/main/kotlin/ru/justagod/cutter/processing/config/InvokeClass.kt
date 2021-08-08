package ru.justagod.cutter.processing.cutter.config

import ru.justagod.cutter.model.ClassTypeReference

class InvokeClass(
    val name: ClassTypeReference,
    val sides: Set<SideName>,
    val functionalMethod: MethodDesc
) {

    override fun toString(): String = "${name.name}.$functionalMethod $sides"

}