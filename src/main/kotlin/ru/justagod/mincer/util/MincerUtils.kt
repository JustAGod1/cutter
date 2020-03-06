package ru.justagod.mincer.util

import org.zeroturnaround.zip.ZipUtil
import ru.justagod.mincer.control.MincerControlPane
import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.util.recursiveness.ByteArraySource
import ru.justagod.mincer.util.recursiveness.MincerFallbackFS
import ru.justagod.mincer.util.recursiveness.MincerTreeFS
import ru.justagod.mincer.util.recursiveness.MincerZipFS
import java.io.File

object MincerUtils {

    fun processFolder(panel: MincerControlPane, folder: File) {
        do {
            for (f in folder.walkTopDown().filter { it.path.endsWith(".class") && it.isFile }) {
                val name = f.absoluteFile.relativeTo(folder.absoluteFile)
                val lastModified = f.lastModified()
                val content = f.readBytes()

                val result = panel.advance(content, name.path, lastModified)
                if (result.type == MincerResultType.MODIFIED) {
                    f.writeBytes(result.resultedBytecode)
                } else if (result.type == MincerResultType.DELETED) {
                    f.delete()
                }
            }
        } while (panel.endIteration())
    }

    private fun readZip(file: File): HashMap<String, ByteArraySource> {
        val result = hashMapOf<String, ByteArraySource>()
        ZipUtil.iterate(file) { input, entry ->
            if (!entry.isDirectory)
                result[entry.name] = ByteArraySource(entry.name, input!!.readBytes())
        }

        return result

    }

    fun processArchive(file: File, factory: MincerFactory): MincerZipFS {
        val archive = MincerZipFS(file, readZip(file))
        val mincer = factory(archive)
        val resultEntries = hashMapOf<String, ByteArraySource>()
        do {
            val iterator = archive.entries.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (!entry.value.path.endsWith(".class")) {
                    resultEntries[entry.key] = entry.value
                    continue
                }

                val result = mincer.advance(entry.value.bytes, entry.value.path, 0)
                if (result.type == MincerResultType.DELETED) {
                    iterator.remove()
                    resultEntries -= entry.key
                } else {
                    resultEntries[entry.key] = ByteArraySource(entry.key, result.resultedBytecode)
                }
            }
            archive.entries.clear()
            archive.entries.putAll(resultEntries)
        } while (mincer.endIteration())


        return archive

    }


}