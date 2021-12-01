package ru.justagod.cutter.processing.model

import ru.justagod.cutter.processing.config.SideName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ProjectModel(private val defaultSides: Set<SideName>) {

    private val lambdaMethods = hashSetOf<MethodAtom>()

    private val executor = Executors.newSingleThreadExecutor()

    private val atoms = ConcurrentHashMap<ProjectAtom, SidesNode>()

    fun atom(atom: ProjectAtom, sides: Set<SideName>?) {
        executor.submit {
            val node = getOrDefault(atom, sides)
            node.sinthetic = false
            if (node.sides != null && sides != null) node.sides!!.retainAll(sides)
            else if (node.sides == null) node.sides = sides?.toHashSet()
        }
    }

    private fun getOrDefault(atom: ProjectAtom, sides: Set<SideName>?) = atoms.computeIfAbsent(atom) {
        SidesNode(true, sides?.toHashSet(), hashSetOf(), atom)
    }

    private fun getOrDefault(atom: ProjectAtom) = atoms.computeIfAbsent(atom) {
        SidesNode(true, null, hashSetOf(), atom)
    }

    fun rememberLambdaMethod(atom: MethodAtom) {
        executor.submit {
            lambdaMethods += atom

        }
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

            var result: HashSet<SideName>? = null
            while (parent != null) {
                if (parent !is FolderAtom) continue
                val parentNode = atoms[parent]
                if (parentNode?.sides != null) {
                    if (result == null)
                        result = parentNode.sides!!.toHashSet()
                    else
                        result.retainAll(parentNode.sides!!)
                }

                parent = parent.parent()
            }

            if (node.sides == null) node.sides = result ?: defaultSides.toHashSet()
            else if (result != null) node.sides!!.retainAll(result)
        }
    }

    fun finish() {
        linkFolders()
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
    }

    private fun checkTasksCompleted() {
        if (!executor.isTerminated) error("Call await first")
    }

    fun sidesFor(atom: ProjectAtom): Set<SideName>? {
        checkTasksCompleted()

        val node = atoms[atom] ?: return null

        if (node.sinthetic) return null
        var result: HashSet<SideName>? = node.sides?.toHashSet()
        // Поиск в ширину сука
        // он преследует меня
        var parents = node.parents.toMutableSet()

        val dejaVu = hashSetOf<SidesNode>()

        while (parents.isNotEmpty()) {
            val new = hashSetOf<SidesNode>()

            for (parent in parents) {
                if (parent in dejaVu) continue
                val sides = parent.sides
                if (sides != null) {
                    if (result == null) result = sides.toHashSet()
                    else result.retainAll(sides)
                }
                new += parent.parents

                dejaVu += parent
            }

            parents = new
        }


        return result ?: defaultSides
    }

    fun isLambda(atom: MethodAtom): Boolean {
        checkTasksCompleted()
        return atom in lambdaMethods
    }

    private class SidesNode(
        var sinthetic: Boolean,
        var sides: HashSet<SideName>?,
        val parents: HashSet<SidesNode>,
        val debug: ProjectAtom
    )
}