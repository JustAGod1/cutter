package ru.justagod.cutter.mincer.util.recursiveness

import org.zeroturnaround.zip.ZipUtil
import ru.justagod.cutter.mincer.control.MincerFS
import java.io.File

class MincerZipFS(private val file: File, val entries: MutableMap<String, ByteArraySource>): MincerFS {

    override fun pushGeneratedClass(path: String, bytecode: ByteArray) {
        entries[path] = ByteArraySource(path, bytecode)
    }

    override fun pullClass(path: String): ByteArray? {
        return entries[path]?.bytes
    }
}
