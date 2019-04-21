package ru.justagod.mincer.processor

import ru.justagod.mincer.pipeline.Pipeline
import org.objectweb.asm.tree.ClassNode
import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory

interface SubMincer<Input : Any, Output : Any> {

    /**
     * @return нужно ли перезаписать класс
     */
    fun process(
            name: ClassTypeReference,
            data: ClassModel?,
            node: ClassNode?,
            pipeline: Pipeline<Input, Output>,
            input: Input,
            inheritance: InheritanceHelper,
            nodes: NodesFactory,
            factory: ModelFactory,
            skipped: Boolean
    ): ProcessingResult

    fun startProcessing(
            input: Input,
            cache: List<ClassTypeReference>?,
            inheritance: InheritanceHelper,
            pipeline: Pipeline<Input, Output>
    ) {
    }

    fun endProcessing(
            input: Input,
            cache: List<ClassTypeReference>?,
            inheritance: InheritanceHelper,
            pipeline: Pipeline<Input, Output>
    ) {
    }
}