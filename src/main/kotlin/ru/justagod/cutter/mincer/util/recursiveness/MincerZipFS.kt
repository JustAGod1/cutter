package ru.justagod.cutter.mincer.util.recursiveness

import ru.justagod.cutter.mincer.control.MincerFS

class MincerZipFS(val entries: MutableMap<String, ByteArraySource>): MincerFS {

    override fun pushGeneratedClass(path: String, bytecode: ByteArray) {
        entries[path] = ByteArraySource(path, bytecode)
    }

    override fun pullClass(path: String): ByteArray? {
        return entries[path]?.bytes
    }
}
