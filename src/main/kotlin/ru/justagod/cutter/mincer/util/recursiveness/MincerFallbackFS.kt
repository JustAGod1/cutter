package ru.justagod.cutter.mincer.util.recursiveness

import ru.justagod.cutter.mincer.control.MincerFS

class MincerFallbackFS(private val delegate: MincerFS, private val fallback: MincerFS): MincerFS {
    override fun pushGeneratedClass(path: String, bytecode: ByteArray) {
        delegate.pushGeneratedClass(path, bytecode)
    }

    override fun pullClass(path: String): ByteArray? {
        return delegate.pullClass(path) ?: fallback.pullClass(path)
    }
}