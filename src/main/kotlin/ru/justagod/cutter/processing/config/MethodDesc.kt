package ru.justagod.cutter.processing.config

import java.io.Serializable

data class MethodDesc(
        val name: String,
        // Like in asm
        val desc: String
) : Serializable {
        override fun toString(): String = name + desc
}