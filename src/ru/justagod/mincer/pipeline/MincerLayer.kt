package ru.justagod.mincer.pipeline

import ru.justagod.mincer.context.FirstPassContext
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.CachedFactory
import ru.justagod.model.factory.ModelFactory
import java.io.File

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
        val unCached = layer
        if (unCached.any { it.second.cacheClearingRequired }) {
            nodes.clear()
            inheritance.clear()
            (factory as? CachedFactory)?.clear()
        }
        if (!caching || unCached.isNotEmpty()) {
            for ((value, pipeline) in unCached) {
                (pipeline.worker as SubMincer<Any, Any>).startProcessing(
                        value,
                        emptyList(),
                        inheritance,
                        pipeline as Pipeline<Any, Any>
                )
            }
            val context = FirstPassContext(factory, nodes, inheritance, root)
            context.process(unCached)
            for ((value, pipeline) in unCached) {
                (pipeline.worker as SubMincer<Any, Any>).endProcessing(
                        value,
                        emptyList(),
                        inheritance,
                        pipeline as Pipeline<Any, Any>
                )
            }
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

}