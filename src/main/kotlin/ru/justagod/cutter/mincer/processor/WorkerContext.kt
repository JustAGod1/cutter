package ru.justagod.cutter.mincer.processor

import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.mincer.pipeline.MincerPipeline
import ru.justagod.cutter.model.ClassTypeReference

class WorkerContext<Input, Output>(
    val name: ClassTypeReference,
    val info: ClassInfo,
    val pipeline: MincerPipeline<Input, Output>,
    val input: Input,
    val mincer: Mincer
) {
    override fun toString(): String {
        return name.toString()
    }
}