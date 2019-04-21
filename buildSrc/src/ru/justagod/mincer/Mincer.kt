package ru.justagod.mincer

import ru.justagod.mincer.pipeline.MincerLayer
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.CachedFactory
import ru.justagod.model.factory.FallbackModelFactory
import java.io.File
import java.util.*

class Mincer private constructor(
        private val root: File,
        private val pipelines: List<Pipeline<*, *>>,
        private val subClassPaths: List<File> = emptyList()
) {
    private val bytecodeFetcher: (String) -> ByteArray? = {
        val name = it.replace('.', '/') + ".class"
        val file = root.resolve(name)
        if (!file.exists()) {
            val subFile = subClassPaths.find {
                val tmp = it.resolve(name)
                tmp.exists()
            }
            if (subFile != null) {
                subFile.resolve(name).readBytes()
            } else null
        } else file.readBytes()
    }
    val factory = CachedFactory(FallbackModelFactory(this.javaClass.classLoader, bytecodeFetcher))
    val inheritance = InheritanceHelper(factory)
    val nodes = NodesFactory(bytecodeFetcher)

    fun process(caching: Boolean) {
        val queues = pipelines.map { fetchPipelineQueue(it) }

        val layer = MincerLayer(root, queues.map { Pair(Unit, it) }, factory, nodes, inheritance)
        layer.process(caching)
    }

    private fun fetchPipelineQueue(pipeline: Pipeline<*, *>): List<Pipeline<*, *>> {
        val result = LinkedList<Pipeline<*, *>>()
        var entry: Pipeline<*, *>? = pipeline
        while (entry != null) {
            result.add(0, entry)
            entry = entry.parent
        }
        return result
    }


    class Builder(private val root: File, private val subPaths: List<File> = emptyList()) {
        private val registry = LinkedList<Pipeline<*, *>>()

        fun registerSubMincer(pipeline: Pipeline<*, *>): Builder {
            registry += pipeline
            return this
        }

        fun build() = Mincer(root, registry, subPaths)
    }
}