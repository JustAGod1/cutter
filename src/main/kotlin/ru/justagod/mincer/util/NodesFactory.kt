package ru.justagod.mincer.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import ru.justagod.model.ClassTypeReference

class NodesFactory(private val harvester: (name: String) -> ByteArray) {

    fun makeNode(reference: ClassTypeReference, flags: Int = 0): ClassNode {
        val bytecode = harvester(reference.name.replace('.', '/') + ".class")
        try {
            return makeNode(bytecode, flags)
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse class ${reference.name}", e)
        }
    }

    private fun makeNode(bytecode: ByteArray, flags: Int = 0): ClassNode {
        val reader = ClassReader(bytecode)

        val node = ClassNode(Opcodes.ASM9)
        reader.accept(node, flags)
        return node
    }


}