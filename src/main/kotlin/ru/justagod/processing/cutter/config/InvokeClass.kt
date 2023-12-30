package ru.justagod.processing.cutter.config

import ru.justagod.model.ClassTypeReference
import java.io.Serializable

class InvokeClass(
    val name: ClassTypeReference,
    val sides: Set<SideName>,
    val functionalMethod: MethodDesc
) : Serializable {

    override fun toString(): String = "${name.name}.$functionalMethod $sides"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InvokeClass

        if (name != other.name) return false
        if (sides != other.sides) return false
        if (functionalMethod != other.functionalMethod) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + sides.hashCode()
        result = 31 * result + functionalMethod.hashCode()
        return result
    }


}