package ru.justagod.mincer.util.recursiveness

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerFS
import java.io.File

class MincerTreeFS(private val root: File, private val leafs: List<MincerFS>): MincerFS {

    override fun pushGeneratedClass(path: String, bytecode: ByteArray) {
        root.resolve(path).writeBytes(bytecode)
    }

    override fun pushArchive(id: String, processedClasses: Set<String>) {}

    override fun pullArchive(id: String): MincerArchive? = null

    override fun pullClass(path: String): ByteArray? {
        val f = root.resolve(path)
        if (f.exists()) return f.readBytes()
        for (leaf in leafs) {
            val bytecode = leaf.pullClass(path)
            if (bytecode != null) return bytecode
        }

        return null
    }

}