package ru.justagod.cutter.utils

import ru.justagod.cutter.model.ClassTypeReference

data class FullMethodReference(val owner: ClassTypeReference, val name: String, val descriptor: String) {

    fun isEqual(owner: String, name: String, descriptor: String): Boolean {
        if (this.owner.name != owner) return false
        if (this.name != name) return false
        if (this.descriptor != descriptor) return false

        return true
    }

    fun isEqual(owner: ClassTypeReference, name: String, descriptor: String): Boolean {
        if (this.owner != owner) return false
        if (this.name != name) return false
        if (this.descriptor != descriptor) return false

        return true
    }

    fun isEqualInternal(owner: String, name: String, descriptor: String): Boolean {
        if (this.owner.internalName != owner) return false
        if (this.name != name) return false
        if (this.descriptor != descriptor) return false

        return true
    }

}