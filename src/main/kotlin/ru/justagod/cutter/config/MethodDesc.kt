package ru.justagod.processing.cutter.config

data class MethodDesc(
        val name: String,
        val desc: String
) {
        override fun toString(): String = name + desc
}