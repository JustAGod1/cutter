package ru.justagod.cutter.mincer.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import ru.justagod.cutter.model.ClassTypeReference

class NodesFactory(private val harvester: (name: String) -> ByteArray) {

    fun makeNode(reference: ClassTypeReference, flags: Int = 0): ClassNode {
        val bytecode = harvester(reference.name.replace('.', '/') + ".class")
        return makeNode(bytecode, flags)
    }
    fun makeNode(bytecode: ByteArray, flags: Int = 0): ClassNode {
        try {
            val reader = ClassReader(bytecode)

            val node = ClassNode(Opcodes.ASM6)
            reader.accept(node, flags)
            return node
        } catch (e: Exception) {
            throw e
        }
    }


}