package ru.justagod.plugin.processing

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.ProcessingResult
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory
import ru.justagod.plugin.data.SideInfo

class CutterMincer(private val targetSides: List<SideInfo>, private val primalSides: Set<SideInfo>): SubMincer<SidesTree, Unit> {
    override fun process(
            name: ClassTypeReference,
            data: ClassModel?,
            node: ClassNode?,
            pipeline: Pipeline<SidesTree, Unit>,
            input: SidesTree,
            inheritance: InheritanceHelper,
            nodes: NodesFactory,
            factory: ModelFactory,
            skipped: Boolean
    ): ProcessingResult {
        val sides = input.get(name.path, primalSides)
        val path = name.path
        if (!targetSides.any { it in sides }) {
            println(name.name + " has been deleted")
            return ProcessingResult.DELETE
        }
        var modified = false
        node!!.fields?.removeIf {
            val fieldSides = input.get(path + ("field " + it.name), primalSides)
            val result = !targetSides.any { it in fieldSides }
            if (result) {
                modified = true
                println(name.name + "." + it.name + " has been discarded")
            }
            result
        }

        node.methods?.removeIf { method ->
            val methodPath = if (method.access and Opcodes.ACC_SYNTHETIC != 0 && method.name.startsWith("lambda")) {
                val author = method.name.split("$")[1]
                path + (author + "()")
            } else path + (method.name + "()")
            val methodSides = input.get(methodPath, primalSides)
            val result = !targetSides.any { it in methodSides }
            if (result) {
                modified = true
                println(name.name + "." + method.name + "() has been discarded")
            }
            result
        }

        return if (modified) ProcessingResult.REWRITE else ProcessingResult.NOOP
    }
}