package ru.justagod.mincer.util

import ru.justagod.mincer.control.MincerFS
import java.io.File

open class MincerDecentFS(val root: File): MincerFS {
    override fun pushGeneratedClass(path: String, bytecode: ByteArray) {
        root.resolve(File(path)).absoluteFile.writeBytes(bytecode)
    }

    fun pushArchive(id: String, processedClasses: List<String>) {
        val file = root.resolve("$id.archive")
        val writer = file.bufferedWriter()
        writer.use {
            for (entry in processedClasses) {
                writer.write(entry)
                writer.newLine()
            }
        }
    }

    override fun pullClass(path: String): ByteArray? {
        val file = root.resolve(path)
        if (!file.exists()) return null

        return file.readBytes()
    }
}