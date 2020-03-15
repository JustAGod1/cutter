package ru.justagod.plugin.processing.pipeline

import org.objectweb.asm.Label
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import ru.justagod.plugin.data.DynSideMarker
import ru.justagod.plugin.data.SideName

class SidlyInstructionsIter(
        private val src: MutableIterator<AbstractInsnNode>,
        private val primalSides: Set<SideName>,
        private val markers: List<DynSideMarker>
) : MutableIterator<Pair<AbstractInsnNode, Set<SideName>>> {

    private val influences = hashMapOf<Label, MutableList<Set<SideName>>>()
    private var currentSides = primalSides
    private var previousNode: AbstractInsnNode?  = null

    override fun hasNext(): Boolean = src.hasNext()

    override fun next(): Pair<AbstractInsnNode, Set<SideName>> {
        val next = src.next()
        var sidesBefore = currentSides
        if (next is JumpInsnNode) {
            if (previousNode != null) {
                for (marker in markers) {
                    val sides = marker.getSides(previousNode!!, next.opcode, currentSides) ?: continue
                    influences.computeIfAbsent(next.label.label) { arrayListOf() } += sides
                }
                updateCurrentSides()
            }
        } else if (next is LabelNode) {
            influences -= next.label
            updateCurrentSides()
            sidesBefore = currentSides
        }

        previousNode = next
        return next to sidesBefore
    }

    private fun updateCurrentSides() {
        currentSides = influences.values.fold(primalSides) { a, b -> b.fold(a) { c, d -> c intersect d } }
    }

    override fun remove() {
        src.remove()
    }


}