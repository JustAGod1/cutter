package ru.justagod.cutter.mincer.control

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.model.ClassTypeReference

/**
 * Represents result of mincer's bytecode processing
 */
class MincerResult(private val mincer: Mincer, private val name: ClassTypeReference, val resultedNode: ClassNode?, val type: MincerResultType) {

    fun lockAndGetBytecode(block: (ByteArray) -> Unit) {
        val bytecode = bytecode()

        mincer.writeLock(name) {
            block(bytecode)
        }
    }

    fun bytecode() = mincer.nodeToBytes(resultedNode!!)

    fun onModification(block: (ByteArray) -> Unit): MincerResult {
        if (type == MincerResultType.MODIFIED) lockAndGetBytecode(block)
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