package ru.justagod.mincer.processor

import org.objectweb.asm.tree.ClassNode
import ru.justagod.mincer.Mincer
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import kotlin.math.min

class ClassInfo(private val mincer: Mincer, private val name: ClassTypeReference, private var node: ClassNode?) {


    fun node(): ClassNode {
        if (node == null) {
            node = mincer.makeNode(name)
        }

        return node!!
    }

    fun nodeOrNull() = node

}