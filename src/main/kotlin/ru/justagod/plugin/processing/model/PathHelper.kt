package ru.justagod.plugin.processing.model

import ru.justagod.model.ClassTypeReference

object PathHelper {

    fun klass(klass: ClassTypeReference): List<String> {
        return if (klass.simpleName != "package-info") {
            klass.path
        } else {
            klass.path.dropLast(1)
        }
    }

    fun method(holder: ClassTypeReference, name: String, desc: String): List<String> {
        val path = klass(holder)
        return path + (name + desc)
    }

    fun field(holder: ClassTypeReference, name: String, desc: String): List<String> {
        val path = klass(holder)
        return path + (name + desc)
    }
}