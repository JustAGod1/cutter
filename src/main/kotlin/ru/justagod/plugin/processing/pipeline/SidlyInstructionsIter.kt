package ru.justagod.plugin.processing.pipeline

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.JumpInsnNode
import ru.justagod.plugin.data.DynSideMarker
import ru.justagod.plugin.data.FlowDirection
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.util.CutterUtils

class SidlyInstructionsIter private constructor(
        private val data: MutableList<AbstractInsnNode>,
        primalSides: Set<SideName>,
        private val markers: List<DynSideMarker>
) : MutableIterator<Pair<AbstractInsnNode, Set<SideName>>> {

    private val delegate = data.iterator()
    private val sides = HashMap<AbstractInsnNode, MutableSet<SideName>>(data.size)


    init {
        if (data.isNotEmpty()) {
            for (primalSide in primalSides) {
                mark(0, primalSide)
            }
            if (System.getProperty("print-sides") == "true") {
                println()
                val maxLen = sides.values.map { it.joinToString(separator = " ") { it.name } }.maxBy { it.length }!!.length
                for (datum in data) {
                    println(String.format("%0\$${maxLen}s  | %s", sides[datum]?.joinToString(separator = " ") { it.name}  ?: "none", CutterUtils.nodeToString(datum)))
                }
            }
        }
    }

    private fun mark(start: Int, side: SideName) {
        var idx = start
        var node = data[idx]
        sides.computeIfAbsent(node) { hashSetOf() }.add(side)
        while (node !is JumpInsnNode && idx < data.size - 1) {
            idx++
            node = data[idx]
            sides.computeIfAbsent(node) { hashSetOf() }.add(side)
        }
        if (idx >= data.size) return

        if (node !is JumpInsnNode) return
        if (node.opcode == Opcodes.GOTO) {
            mark(data.indexOf(node.label), side)
            return
        }
        var direction = FlowDirection.BOTH
        for (marker in markers) {
            val newDirection = marker.getDirection(data[idx - 1], node.opcode, side)
            if (direction == FlowDirection.BOTH) direction = newDirection
            else if (direction != newDirection && newDirection != FlowDirection.BOTH)
                error("Dynamic markers have made opposite decisions about code flow. Please check your config.")
        }

        if (direction == FlowDirection.ALWAYS_JUMP || direction == FlowDirection.BOTH) {
            mark(data.indexOf(node.label), side)
        }
        if (direction == FlowDirection.ALWAYS_PASS || direction == FlowDirection.BOTH) {
            mark(idx + 1, side)
        }
    }

    override fun hasNext(): Boolean {
        return delegate.hasNext()
    }

    override fun next(): Pair<AbstractInsnNode, Set<SideName>> {
        val next = delegate.next()
        return next to (sides[next] ?: emptySet<SideName>())
    }

    override fun remove() {
        delegate.remove()
    }


    companion object {
        fun iterate(
                instructions: InsnList,
                initialSides: Set<SideName>,
                markers: List<DynSideMarker>,
                acceptor: (Pair<AbstractInsnNode, Set<SideName>>) -> Unit
        ) {
            val data = instructions.iterator().asSequence().toMutableList()
            val iter = SidlyInstructionsIter(data, initialSides, markers)
            iter.forEach(acceptor)
        }

        fun iterateAndTransform(
                instructions: InsnList,
                initialSides: Set<SideName>,
                markers: List<DynSideMarker>,
                acceptor: (Pair<AbstractInsnNode, Set<SideName>>) -> Boolean
        ) {
            val data = instructions.iterator().asSequence().toMutableList()
            val iter = SidlyInstructionsIter(data, initialSides, markers)
            while (iter.hasNext()) {
                val e = iter.next()
                if (!acceptor(e)) iter.remove()
            }

            instructions.clear()
            data.forEach(instructions::add)


        }
    }

}