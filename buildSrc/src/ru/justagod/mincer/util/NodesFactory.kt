package ru.justagod.mincer.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import ru.justagod.model.ClassTypeReference
import java.io.File

class NodesFactory(private val bytecodeProvider: (String) -> ByteArray?) {

    private val cache = hashMapOf<String, ByteArray>()

    fun makeNode(file: File): ClassNode {
        val bytecode = cache.computeIfAbsent(file.path) {
            file.readBytes()
        }
        return makeNode(bytecode)
    }

    fun makeNode(bytecode: ByteArray): ClassNode {
        val reader = ClassReader(bytecode)
        val node = ClassNode(Opcodes.ASM6)
        reader.accept(node, 0)
        return node
    }

    fun makeNode(type: ClassTypeReference): ClassNode {
        val bytecode = if (type.name in cache) {
            cache[type.name]!!
        } else {
            val provided = bytecodeProvider(type.name) ?: error("Unable to find bytecode for $type =((")
            cache[type.name] = provided
            provided
        }

        return makeNode(bytecode)
    }

    fun clear() {
        cache.clear()
    }

}