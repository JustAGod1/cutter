package ru.justagod.mincer.util.recursiveness

import org.zeroturnaround.zip.ZipEntrySource
import java.io.InputStream
import java.util.zip.ZipEntry

class ByteArraySource(private val path: String, val bytes: ByteArray) : ZipEntrySource {
    override fun getPath(): String {
        return path
    }

    override fun getEntry(): ZipEntry {
        val entry = ZipEntry(path)
        entry.size = bytes.size.toLong()
        return entry
    }

    override fun getInputStream(): InputStream {
        return bytes.inputStream()
    }
}