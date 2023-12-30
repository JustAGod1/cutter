package ru.justagod.processing.cutter.config

import java.io.Serializable

data class MethodDesc(
        val name: String,
        val desc: String
) : Serializable {
        override fun toString(): String = name + desc
}