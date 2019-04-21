package ru.justagod.mincer.context

import com.google.common.collect.HashMultimap
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory
import java.io.File

class CachedContext(
        factory: ModelFactory,
        inheritance: InheritanceHelper,
        nodes: NodesFactory,
        root: File
) : MincerContext(factory, inheritance, nodes, root) {

    fun process(mincers: Map<Triple<Any, Pipeline<*, *>, Long>, Collection<String>>): Map<String, Collection<File>> {
        val result = HashMultimap.create<String, File>()
        for ((data, files) in mincers) {
            for (file in root.walkTopDown().filter { it.isFile }.filter { it.name.endsWith(".class") }) {
                if (file.absolutePath in files && (data.second.skippable && file.lastModified() < data.third)) {
                    accept(file, data.second, data.first, true)
                    result.put(data.second.id, file)
                } else if (file.lastModified() > data.third || !data.second.skippable) {
                    val valid = file.absolutePath in files || try {
                        val name = ClassTypeReference((file.absolutePath.drop(root.absolutePath.length + 1).dropLast(6).replace("[/\\\\]".toRegex(), ".")))
                        val model = {
                            factory.makeModel(
                                    name,
                                    null
                            )
                        }
                        data.second.filter.isValid(name, model, inheritance, factory)
                    } catch (e: Exception) {
                        false
                    }
                    if (valid) {
                        result.put(data.second.id, file)
                        accept(file, data.second, data.first, false)
                    }
                }
            }
        }
        return result.asMap()!!
    }

}
