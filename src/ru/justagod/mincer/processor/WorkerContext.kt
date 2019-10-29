package ru.justagod.mincer.processor

import org.objectweb.asm.tree.ClassNode
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference

class WorkerContext<Input: Any, Output: Any>(
        val name: ClassTypeReference,
        val info: ClassInfo?,
        val pipeline: Pipeline<Input, Output>,
        val input: Input,
        val mincer: Mincer
)