package ru.justagod.mincer.util.recursiveness

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.util.MincerDecentFS
import ru.justagod.model.ClassTypeReference
import java.io.File

class MincerTreeFS(root: File, private val leafs: List<MincerFS>): MincerDecentFS(root) {

    override fun pullClass(path: String): ByteArray? {
        val f = root.resolve(path)
        if (f.exists()) {
            return f.readBytes()
        }
        for (leaf in leafs) {
            val bytecode = leaf.pullClass(path)
            if (bytecode != null) return bytecode
        }

        return null
    }

}