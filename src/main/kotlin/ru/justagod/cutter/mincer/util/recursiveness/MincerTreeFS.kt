package ru.justagod.cutter.mincer.util.recursiveness

import ru.justagod.cutter.mincer.control.MincerFS
import ru.justagod.cutter.mincer.util.MincerDecentFS
import java.io.File

class MincerTreeFS(root: File, private val leafs: List<MincerFS>): MincerDecentFS(root) {

    override fun pullClass(path: String): ByteArray? {
        val f = root.resolve(path)
        if (f.exists()) {
            val input = f.inputStream()
            return input.readBytes()
        }
        for (leaf in leafs) {
            val bytecode = leaf.pullClass(path)
            if (bytecode != null) return bytecode
        }

        return null
    }

}