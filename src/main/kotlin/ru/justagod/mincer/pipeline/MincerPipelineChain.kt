package ru.justagod.mincer.pipeline

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.control.MincerResult
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.ClassInfo
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.OBJECT_REFERENCE
import ru.justagod.model.factory.BytecodeModelFactory
import ru.justagod.utils.cast
import kotlin.reflect.jvm.jvmName

class MincerPipelineChain<Output>(
    val pipeline: MincerPipeline<*, *>,
    val input: Any?,
    val next: RawChainSegment?
) {

    fun advance(input: Any?): MincerPipelineChain<Output>? {
        if (next == null) return null
        return MincerPipelineChain(next.pipeline, input, next.next)
    }

    fun process(mincer: Mincer, node: ClassNode?, name: ClassTypeReference): MincerResult {
        try {
            if (checkValidity(mincer, name))
                return doProcess(mincer, node, name)
            return MincerResult(mincer, node, MincerResultType.SKIPPED)
        } catch (e: Exception) {
            throw RuntimeException("Exception while processing $name", e)
        }

    }

    private fun checkValidity(mincer: Mincer, name: ClassTypeReference): Boolean {
        return pipeline.filter.isValid(name, mincer)
    }

    private fun doProcess(mincer: Mincer, node: ClassNode?, name: ClassTypeReference): MincerResult {
        val pipeline = pipeline.cast<MincerPipeline<Any?, Any?>>()
        val info = ClassInfo(mincer, name, node)

        val result = pipeline.worker
            .process(
                WorkerContext(
                    name,
                    info,
                    pipeline,
                    input,
                    mincer
                )
            )
        if (result == MincerResultType.MODIFIED && info.nodeOrNull() == null)
            error("Exception in ${pipeline.worker::class.jvmName}. Modified is true but node wasn't even requested.")

        if (mincer.debug && result == MincerResultType.MODIFIED) {
            try {
                mincer.nodeToBytes(info.nodeOrNull()!!)
            } catch (e: Exception) {
                throw RuntimeException("Exception while writing node after ${pipeline.worker.javaClass.name}")
            }
        }

        return MincerResult(mincer, info.nodeOrNull(), result)
    }





}