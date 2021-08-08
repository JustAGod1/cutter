package ru.justagod.mincer.pipeline

import org.objectweb.asm.tree.ClassNode
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.filter.SingleClassFilter
import ru.justagod.model.ClassTypeReference
import ru.justagod.utils.cast

class MincerPipelineController<Output>(head: MincerPipelineChain<Output>) {

    private var started = false

    private var head: MincerPipelineChain<Output>? = head

    private var finished = false
    private var result: Output? = null

    fun result() : Output {
        if (!finished) error("Pipeline wasn't finished yet")
        return result.cast()
    }

    fun start() {
        if (started) error("Already started pipeline")
        start(Unit)
        started = true
    }
    private fun start(input: Any?) {
        val pipeline = head!!.pipeline.cast<MincerPipeline<Any?, Any?>>()
        pipeline.output = pipeline.worker
            .startProcessing(
                input,
                pipeline.output
            )
    }

    fun targetClass() = (head?.pipeline?.filter as? SingleClassFilter)?.target

    fun process(mincer: Mincer, node: ClassNode?, name: ClassTypeReference) = head!!.process(mincer, node, name)

    fun advance(): Boolean {
        val head = head!!
        val pipeline = head.pipeline.cast<MincerPipeline<Any?, Any?>>()
        pipeline.output = pipeline.worker.endProcessing(
            head.input,
            pipeline.output
        )
        val newHead = head.advance(head.pipeline.output)

        if (newHead != null) {
            this.head = newHead
            start(head.pipeline.output)
            return true
        } else {
            result = head.pipeline.output.cast<Output>()
            finished = true
            return false
        }
    }



}