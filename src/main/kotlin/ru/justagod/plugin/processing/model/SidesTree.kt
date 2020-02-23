package ru.justagod.plugin.processing.model

import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.util.intersection
import java.lang.Exception

class SidesTree(val name: String) {

    private var sides: MutableSet<SideName>? = null

    private val children = hashMapOf<String, SidesTree>()

    operator fun get(name: String) = children[name]

    fun set(sides: Set<SideName>?) {
        if (this.sides == null) {
            this.sides = sides?.toMutableSet()
        } else if (sides != null) {
            this.sides = this.sides!!.intersection(sides).toMutableSet()
        }
    }

    fun identify(valid: Set<SideName>?) {
        if (valid != null) sides?.removeIf { it !in valid }
        children.values.forEach { it.identify(sides) }
    }

    fun set(path: List<String>, offset: Int, sides: Set<SideName>?) {
        if (offset == path.size) {
            set(sides)
            return
        }
        val entry = path[offset]
        if (entry in children) {
            val node = this[entry]!!
            node.set(path, offset + 1, sides)
        } else {
            val node = SidesTree(entry)
            node.set(path, offset + 1, sides)
            children[entry] = node
        }
    }

    fun set(path: List<String>, sides: Set<SideName>?) {
        set(path, 0, sides)
    }

    fun get(path: List<String>, primal: Set<SideName>): Set<SideName> = get(path, 0, primal)

    fun get(path: List<String>, offset: Int = 0, primal: Set<SideName>): Set<SideName> {
        if (path.size == offset) return sides ?: primal
        val entry = path[offset]
        val newPrimal = if (this.sides != null) primal.filter { it in sides!! }.toSet() else primal
        return if (entry in children) children[entry]?.get(path, offset + 1, newPrimal) ?: primal
        else newPrimal
    }


    fun toString(primal: Set<SideName>, offset: String = ""): String {
        val builder = StringBuilder()
        builder.append("\u001B[36m").append(name).append("\u001B[0m ")
        if (sides != null) {
            if (sides!!.isNotEmpty()) {
                sides!!.joinTo(builder, separator = " ") { it.name }
            } else {
                builder.append("\u001B[31m").append("DEAD").append("\u001B[0m")
            }

        }
        val newPrimal = if (this.sides != null) primal.filter { it in sides!! }.toSet() else primal
        for (child in children.values) {
            builder.append("\n")
            builder.append(offset).append("\u001B[33m+--\u001B[0m")
            builder.append(child.toString(newPrimal, "$offset\u001B[33m   \u001B[0m"))
        }

        return builder.toString()
    }
}