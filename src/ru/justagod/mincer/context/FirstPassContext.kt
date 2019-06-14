package ru.justagod.mincer.context

import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory
import java.io.File

class FirstPassContext(
        factory: ModelFactory,
        nodes: NodesFactory,
        inheritance: InheritanceHelper,
        root: File
) : MincerContext(factory, inheritance, nodes, root) {

    fun process(mincers: List<Pair<Any, Pipeline<*, *>>>): Map<String, Collection<File>> {
        val result = hashMapOf<String, MutableList<File>>()
        for (file in root.walkTopDown().filter { it.isFile }.filter { it.name.endsWith(".class") }) {
            val name = ClassTypeReference((file.absolutePath.drop(root.absolutePath.length + 1).dropLast(6).replace("[/\\\\]".toRegex(), ".")))
            val model = {
                factory.makeModel(
                        name,
                        null
                )
            }
            for ((input, entry) in mincers) {
                try {
                    val valid = try {
                        entry.filter.isValid(name, model, inheritance, factory)
                    } catch (e: Exception) {
                        false
                    }
                    if (valid) {
                        accept(file, entry, input, false)
                        result.computeIfAbsent(entry.id) { arrayListOf() } += file
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return result
    }
}