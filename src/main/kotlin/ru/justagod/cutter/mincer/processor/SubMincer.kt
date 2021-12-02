package ru.justagod.cutter.mincer.processor

import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.Mincer


/**
 * It's main task force of the [Mincer]. And also it's only use of this.
 * So just read [Mincer] documentation to know what is it
 */
interface SubMincer<Input, Output> {

    /**
     * Mincer pass every class that wasn't filtered out here.
     *
     * @param context useful information and mincer instance
     */
    fun process(context: WorkerContext<Input, Output>): MincerResultType

    /**
     * Called when this sub mincer becomes active in corresponding pipeline.
     *
     * @param input result of previous sub mincer
     * @param output current output of corresponding pipeline segment. Most likely it will be default value.
     * @return new output
     */
    fun startProcessing(
            input: Input,
            output: Output
    ) = output

    /**
     * Called when [Mincer.endIteration] was called and this sub mincer was active. Usually it means that all
     * classes were processed
     *
     * @param input result of previous sub mincer
     * @param output current output of corresponding pipeline segment.
     * @return new output
     */
    fun endProcessing(
            input: Input,
            output: Output
    )  = output
}