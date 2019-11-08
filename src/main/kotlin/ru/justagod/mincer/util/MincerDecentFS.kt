package ru.justagod.mincer.util

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerFS
import ru.justagod.model.ClassTypeReference
import java.io.File

class MincerDecentFS(val root: File): MincerFS {
    override fun pushGeneratedClass(path: String, bytecode: ByteArray) {
        root.resolve(File(path)).absoluteFile.writeBytes(bytecode)
    }

    override fun pushArchive(id: String, processedClasses: Set<String>) {
        val file = root.resolve("$id.archive")
        val writer = file.bufferedWriter()
        writer.use {
            for (entry in processedClasses) {
                writer.write(entry)
                writer.newLine()
            }
        }
    }

    override fun pullArchive(id: String): MincerArchive? {
        val file = root.resolve("$id.archive")
        if (!file.exists()) return null
        val lastModified = file.lastModified()
        val members = file.readLines().map { ClassTypeReference(it) }
        return MincerArchive(members, lastModified)
    }

    override fun pullClass(path: String): ByteArray? {
        val file = root.resolve(path)
        if (!file.exists()) return null

        return file.readBytes()
    }
}