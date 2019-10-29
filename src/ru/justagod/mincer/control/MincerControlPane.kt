package ru.justagod.mincer.control

interface MincerControlPane {

    fun advance(source: ByteArray, name: String, lastModified: Long): MincerResult

    fun endIteration(): Boolean
}