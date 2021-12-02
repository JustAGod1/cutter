package ru.justagod.cutter.mincer.pipeline

import org.objectweb.asm.tree.ClassNode
import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.mincer.filter.SingleClassFilter
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.utils.cast

/**
 * Instance of this class you can obtain from [MincerPipeline.build]
 *
 * This class represents constructed chain of sub mincers. Inner representations is kinda mess.
 * But all you need to know that it is a chain of sub mincers with some utility methods.
 *
 * You may use it to obtain pipeline result. Like:
 * ```
 * val pipeline = Pipeline.make(...).build()
 * val mincer = ...
 * MincerUtils.processFolder(...)
 *
 * println(pipeline.result())
 * ```
 */
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