package ru.justagod.mincer.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import ru.justagod.mincer.control.MincerFS
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.factory.BytecodeModelFactory
import java.io.File

class NodesFactory(private val harvester: (name: String) -> ByteArray) {

    private val cache = hashMapOf<ClassTypeReference, ByteArray>()

    fun makeNode(reference: ClassTypeReference): ClassNode {
        val bytecode = if (reference in cache) cache[reference]!!
        else harvester(reference.name.replace('.', '/') + ".class")
        val reader = ClassReader(bytecode)
        val node = ClassNode(Opcodes.ASM5)
        reader.accept(node, 0)
        return node
    }

    companion object {

        fun rootDiscoverer(root: File) = NodesFactory {
            val file = root.resolve(it)
            if (!file.exists()) throw BytecodeModelFactory.BytecodeNotFoundException(it)
            file.readBytes()
        }
    }

}