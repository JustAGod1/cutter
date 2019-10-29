package ru.justagod.mincer.processor

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.model.InheritanceHelper

interface SubMincer<Input : Any, Output : Any> {

    /**
     * @return нужно ли перезаписать класс
     */
    fun process(context: WorkerContext<Input, Output>): MincerResultType

    fun startProcessing(
            input: Input,
            cache: MincerArchive?,
            inheritance: InheritanceHelper,
            pipeline: Pipeline<Input, Output>
    ) {
    }

    fun endProcessing(
            input: Input,
            cache: MincerArchive?,
            inheritance: InheritanceHelper,
            pipeline: Pipeline<Input, Output>
    ) {
    }
}