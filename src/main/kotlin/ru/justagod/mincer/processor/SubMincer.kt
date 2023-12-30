package ru.justagod.mincer.processor

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.MincerPipeline
import ru.justagod.model.InheritanceHelper

interface SubMincer<Input, Output> {

    /**
     * @return нужно ли перезаписать класс
     */
    fun process(context: WorkerContext<Input, Output>): MincerResultType

    fun startProcessing(
            input: Input,
            output: Output
    ) = output

    fun endProcessing(
            input: Input,
            output: Output
    )  = output
}