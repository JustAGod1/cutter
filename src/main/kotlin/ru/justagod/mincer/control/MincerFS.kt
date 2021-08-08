package ru.justagod.mincer.control

interface MincerFS {

    fun pushGeneratedClass(path: String, bytecode: ByteArray)

    fun pullClass(path: String): ByteArray?

}