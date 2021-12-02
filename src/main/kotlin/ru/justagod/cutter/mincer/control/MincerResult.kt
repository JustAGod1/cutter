package ru.justagod.cutter.mincer.control

import org.objectweb.asm.tree.ClassNode
import ru.justagod.cutter.mincer.Mincer

/**
 * Represents result of mincer's bytecode processing
 */
class MincerResult(private val mincer: Mincer, val resultedNode: ClassNode?, val type: MincerResultType) {

    /**
     * Just bytecode of resulted node. Throws NPE if class was deleted
     */
    fun bytecode() = mincer.nodeToBytes(resultedNode!!)


    // Just internal method to merge results of different sub mincers
    infix fun merge(other: MincerResult): MincerResult {
        if (other.type == MincerResultType.SKIPPED) return this
        return other
    }

    // Handy methods starts here

    fun onModification(block: (ByteArray) -> Unit): MincerResult {
        if (type == MincerResultType.MODIFIED) block(bytecode())
        return this
    }

    fun onDeletion(block: () -> Unit): MincerResult {
        if (type == MincerResultType.DELETED) block()
        return this
    }




}