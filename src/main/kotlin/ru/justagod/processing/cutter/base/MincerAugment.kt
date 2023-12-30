package ru.justagod.processing.cutter.base

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.WorkerContext

abstract class MincerAugment<Input, Output> {

    abstract fun process(context: WorkerContext<Input, Output>): MincerResultType

}