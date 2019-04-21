package ru.justagod.mincer.pipeline

import ru.justagod.mincer.context.CachedContext
import ru.justagod.mincer.context.FirstPassContext
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

class MincerLayer(
        private val root: File,
        private val pipelines: List<Pair<Any, List<Pipeline<*, *>>>>,
        private val factory: ModelFactory,
        private val nodes: NodesFactory,
        private val inheritance: InheritanceHelper
) {

    private val layer = pipelines.map { Pair(it.first, it.second[0]) }

    @Suppress("UNCHECKED_CAST")
    fun process(caching: Boolean) {
        fun makeReference(name: String): ClassTypeReference {
            return ClassTypeReference((name.drop(root.absolutePath.length + 1).dropLast(6).replace("[/\\\\]".toRegex(), ".")))
        }

        val cache = fetchCache()
        val unCached = if (caching)
            layer.filterNot { pipeline -> cache.any { it.key.second.id == pipeline.second.id } }
        else
            layer
        if (!caching || unCached.isNotEmpty()) {
            val tmpCache = cache.mapKeys { it.key.second.id }
            for ((value, pipeline) in unCached) {
                (pipeline.worker as SubMincer<Any, Any>).startProcessing(
                        value,
                        tmpCache[pipeline.id]?.map(::makeReference),
                        inheritance,
                        pipeline as Pipeline<Any, Any>
                )
            }
            val context = FirstPassContext(factory, nodes, inheritance, root)
            val result = context.process(unCached)
            for ((value, pipeline) in unCached) {
                (pipeline.worker as SubMincer<Any, Any>).endProcessing(
                        value,
                        tmpCache[pipeline.id]?.map(::makeReference),
                        inheritance,
                        pipeline as Pipeline<Any, Any>
                )
            }
            saveCache(result)
        }
        if (caching && cache.isNotEmpty()) {
            for ((data, files) in cache) {
                (data.second.worker as SubMincer<Any, Any>).startProcessing(
                        data.first,
                        files.map { makeReference(it) },
                        inheritance,
                        data.second as Pipeline<Any, Any>
                )
            }
            val context = CachedContext(factory, inheritance, nodes, root)
            val result = context.process(cache)
            for ((data, files) in cache) {
                (data.second.worker as SubMincer<Any, Any>).endProcessing(
                        data.first,
                        files.map { makeReference(it) },
                        inheritance,
                        data.second as Pipeline<Any, Any>
                )
            }
            saveCache(result)
        }
        val nextLayer = pipelines
                .asSequence()
                .map { Pair(it.second[0].value!!, it.second.drop(1)) }
                .filter { it.second.isNotEmpty() }
                .toList()

        if (nextLayer.isEmpty()) return
        val layer = MincerLayer(root, nextLayer, factory, nodes, inheritance)
        layer.process(caching)
    }

    // Map<Triple<Pipeline Input, Pipeline, Last Modifying time>, Files>
    private fun fetchCache(): Map<Triple<Any, Pipeline<*, *>, Long>, Set<String>> =
            layer
                    .groupBy { it }
                    .mapValues { it.value[0] }
                    .map {
                        val file = root.resolve(it.value.second.id + ".map")
                        val entries = if (file.exists()) {
                            file.readLines()
                                    .map { root.resolve(File(it).also { assert(it.exists()) }).absolutePath }.toSet()
                        } else null
                        Pair(Triple(it.key.first, it.key.second, file.lastModified()), entries)
                    }
                    .toMap()
                    .filterValues { it != null }
                    as Map<Triple<Any, Pipeline<*, *>, Long>, Set<String>>

    private fun saveCache(cache: Map<String, Collection<File>>) {
        for ((id, files) in cache) {
            PrintStream(FileOutputStream(root.resolve(File("$id.map")))).use {
                for (file in files) {
                    it.println(file.absolutePath.substring(root.absolutePath.length + 1))
                }
            }
        }
    }
}