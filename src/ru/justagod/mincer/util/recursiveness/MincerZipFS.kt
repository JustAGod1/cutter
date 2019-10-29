package ru.justagod.mincer.util.recursiveness

import org.zeroturnaround.zip.ZipUtil
import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerFS
import java.io.File

class MincerZipFS(private val file: File, val entries: MutableMap<String, ByteArraySource>): MincerFS {

    override fun pushGeneratedClass(path: String, bytecode: ByteArray) {
        entries[path] = ByteArraySource(path, bytecode)
    }

    override fun pushArchive(id: String, processedClasses: Set<String>) {
    }

    override fun pullArchive(id: String): MincerArchive? {
        return null
    }

    override fun pullClass(path: String): ByteArray? {
        return entries[path]?.bytes
    }

    fun commit() {
        ZipUtil.pack(entries.values.toTypedArray(), file)
    }
}