package ru.justagod.mincer.control

interface MincerFS {

    fun pushGeneratedClass(path: String, bytecode: ByteArray)

    fun pushArchive(id: String, processedClasses: Set<String>)

    fun pullArchive(id: String): MincerArchive?

    fun pullClass(path: String): ByteArray?

}