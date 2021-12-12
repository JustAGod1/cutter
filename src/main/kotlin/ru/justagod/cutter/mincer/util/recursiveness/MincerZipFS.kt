package ru.justagod.cutter.mincer.util.recursiveness

import org.zeroturnaround.zip.ZipEntrySource
import org.zeroturnaround.zip.ZipInfoCallback
import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.Zips
import ru.justagod.cutter.mincer.control.MincerFS
import java.io.File
import java.util.zip.ZipEntry

class MincerZipFS(private val file: File): MincerFS {

    private val entries : Map<String, ZipEntry>

    init {
        val entries = hashMapOf<String, ZipEntry>()

        ZipUtil.iterate(file, ZipInfoCallback {
            entries[it.name] = it
        })

        this.entries = entries
    }

    override fun pushGeneratedClass(path: String, bytecode: ByteArray) {
    }

    override fun pullClass(path: String): ByteArray? {
        return Zips.get(file).getEntry(entries[path]?.name ?: return null)
    }
}
