package ru.justagod.mincer.processor

import ru.justagod.mincer.Mincer
import ru.justagod.mincer.pipeline.MincerPipeline
import ru.justagod.model.ClassTypeReference

class WorkerContext<Input, Output>(
        val name: ClassTypeReference,
        val info: ClassInfo,
        val pipeline: MincerPipeline<Input, Output>,
        val input: Input,
        val mincer: Mincer
)