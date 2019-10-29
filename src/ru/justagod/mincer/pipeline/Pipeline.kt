package ru.justagod.mincer.pipeline

import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.filter.ClassFilter
import ru.justagod.mincer.processor.SubMincer

class Pipeline<in Input : Any, Output : Any> private constructor(
        val id: String,
        val worker: SubMincer<in Input, Output>,
        internal val filter: ClassFilter,
        internal val parent: Pipeline<*, in Input>? = null,
        default: Output?,
        internal val skippable: Boolean = true
) {

    var value: Output? = default

    companion object {

        @JvmStatic
        fun <Input : Any, Output : Any> make(
                id: String,
                worker: SubMincer<in Input, Output>,
                filter: ClassFilter,
                parent: Pipeline<*, out Input>,
                default: Output? = null,
                skippable: Boolean = true
        ) = Pipeline(id, worker, filter, parent, default, skippable)

        @JvmStatic
        fun <Output : Any> makeFirst(
                id: String,
                worker: SubMincer<Unit, Output>,
                filter: ClassFilter,
                default: Output? = null,
                skippable: Boolean = true
        ): Pipeline<Unit, Output> = Pipeline(id, worker, filter, null, default, skippable)
    }

    fun unwind(fs: MincerFS): ChainHeadQueue {
        val plainQueue = ArrayList<Pipeline<*, *>>()
        var pipeline: Pipeline<*, *>? = this
        while (pipeline != null) {
            plainQueue.add(0, pipeline)
            pipeline = pipeline.parent
        }

        val topPipeline = plainQueue[0]
        return if (plainQueue.size > 1) ChainHeadQueue(
                topPipeline,
                Unit,
                fs.pullArchive(topPipeline.id),
                buildChain(plainQueue, 1)
        )
        else ChainHeadQueue(topPipeline, Unit, fs.pullArchive(topPipeline.id), null)
    }

    private fun buildChain(plainQueue: List<Pipeline<*, *>>, index: Int): RawChainSegment {
        val next = if (index < plainQueue.lastIndex) buildChain(plainQueue, index + 1) else null
        return RawChainSegment(plainQueue[index], next)
    }
}