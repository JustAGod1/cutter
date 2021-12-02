package ru.justagod.cutter.mincer.processor

import org.objectweb.asm.tree.ClassNode
import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.model.ClassTypeReference

class ClassInfo(private val mincer: Mincer, private val name: ClassTypeReference, private var node: ClassNode?) {


    fun node(): ClassNode {
        if (node == null) {
            node = mincer.makeNode(name)
        }

        return node!!
    }

    fun nodeOrNull() = node

}