package ru.justagod.cutter.mincer.control

import org.objectweb.asm.tree.ClassNode
import ru.justagod.cutter.mincer.Mincer

class MincerResult(private val mincer: Mincer, val resultedNode: ClassNode?, val type: MincerResultType) {

    fun bytecode() = mincer.nodeToBytes(resultedNode!!)

    fun onModification(block: (ByteArray) -> Unit): MincerResult {
        if (type == MincerResultType.MODIFIED) block(bytecode())
        return this
    }

    fun onDeletion(block: () -> Unit): MincerResult {
        if (type == MincerResultType.DELETED) block()
        return this
    }

    infix fun merge(other: MincerResult): MincerResult {
        if (other.type == MincerResultType.SKIPPED) return this
        return other
    }



}