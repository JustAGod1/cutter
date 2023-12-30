package ru.justagod.processing.cutter.model

import ru.justagod.model.ClassTypeReference
import ru.justagod.processing.cutter.config.SideName
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ProjectModel(private val defaultSides: Set<SideName>) {

    private val executor = Executors.newSingleThreadExecutor()

    private val atoms = ConcurrentHashMap<ProjectAtom, SidesNode>()

    fun atom(atom: ProjectAtom, sides: Set<SideName>?) {
        executor.submit {
            try {
                val node = getOrDefault(atom, sides)
                node.sinthetic = false
                if (node.sides != null && sides != null) node.sides = conjunct(node.sides!!, sides)
                else if (node.sides == null) node.sides = sides?.toHashSet()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getOrDefault(atom: ProjectAtom, sides: Set<SideName>?) = atoms.computeIfAbsent(atom) {
        SidesNode(true, sides, hashSetOf(), atom)
    }

    private fun getOrDefault(atom: ProjectAtom) = atoms.computeIfAbsent(atom) {
        SidesNode(true, null, hashSetOf(), atom)
    }

    fun join(child: ProjectAtom, parent: ProjectAtom) {
        if (child == parent) return
        executor.submit {
            val childNode = getOrDefault(child)
            val parentNode = getOrDefault(parent)

            childNode.parents += parentNode
        }
    }

    private fun linkFolders() {
        for ((atom, node) in atoms) {
            if (atom !is ClassAtom || node.sinthetic) continue
            var parent: ProjectAtom? = atom.parent()

            var result: Set<SideName>? = null
            while (parent != null) {
                if (parent !is FolderAtom) continue
                val parentNode = atoms[parent]
                if (parentNode?.sides != null) {
                    if (result == null)
                        result = parentNode.sides!!.toHashSet()
                    else
                        result = conjunct(parentNode.sides!!, result)
                }

                parent = parent.parent()
            }

            if (node.sides == null) node.sides = result ?: defaultSides.toHashSet()
            else if (result != null) node.sides = conjunct(result, node.sides!!)
        }
    }

    fun finish() {
        linkFolders()
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
    }

    private fun checkTasksCompleted() {
        if (!executor.isTerminated) error("Call finish() first")
    }

    fun sidesFor(atom: ProjectAtom): Set<SideName>? {
        checkTasksCompleted()

        val node = atoms[atom] ?: return null

        if (node.sinthetic) return null
        var result: Set<SideName>? = node.sides?.toHashSet()
        var parents = node.parents.toMutableSet()

        val dejaVu = hashSetOf<SidesNode>()

        while (parents.isNotEmpty()) {
            val new = hashSetOf<SidesNode>()

            for (parent in parents) {
                if (parent in dejaVu) continue
                val sides = parent.sides
                if (sides != null) {
                    result = if (result == null) sides.toHashSet()
                    else conjunct(sides, result)
                }
                new += parent.parents

                dejaVu += parent
            }

            parents = new
        }


        return result ?: defaultSides
    }

    private fun conjunct(leftHand: Set<SideName>, rightHand: Set<SideName>): Set<SideName> {
        val transitions = hashMapOf<SideName, SideName>()

        for (side in rightHand) {
            for (parent in side.parents) {
                transitions[parent] = side
            }
        }

        for (side in rightHand) {
            transitions[side] = side
        }

        val result = hashSetOf<SideName>()
        for (side in leftHand) {
            val transitioned = transitions[side] ?: continue
            result += transitioned
        }

        return result
    }

    private class SidesNode(
        var sinthetic: Boolean,
        var sides: Set<SideName>?,
        val parents: HashSet<SidesNode>,
        val debug: ProjectAtom
    )
}