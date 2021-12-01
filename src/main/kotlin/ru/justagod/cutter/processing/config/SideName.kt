package ru.justagod.cutter.processing.config

import java.io.Serializable

class SideName private constructor(val name: String) : Serializable {


    companion object {
        private val cache = hashMapOf<String, SideName>()

        @JvmStatic
        fun make(name: String): SideName {
            return cache.computeIfAbsent(name.toLowerCase()) { SideName(name.toLowerCase()) }
        }
    }



    override fun toString(): String = "Side($name)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SideName

        if (!name.equals(other.name, ignoreCase = true)) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}