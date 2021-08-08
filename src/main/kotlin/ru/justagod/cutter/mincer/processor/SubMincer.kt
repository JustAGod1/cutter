package ru.justagod.cutter.mincer.processor

import ru.justagod.cutter.mincer.control.MincerResultType

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