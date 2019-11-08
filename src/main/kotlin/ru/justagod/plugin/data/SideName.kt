package ru.justagod.plugin.data

class SideName private constructor(val name: String) {


    companion object {
        private val cache = hashMapOf<String, SideName>()

        internal fun make(name: String): SideName {
            return cache.computeIfAbsent(name) { SideName(name) }
        }
    }

}