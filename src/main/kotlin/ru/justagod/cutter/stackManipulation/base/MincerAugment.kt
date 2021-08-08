package ru.justagod.cutter.processing.cutter.base

import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.processor.WorkerContext

abstract class MincerAugment<Input, Output> {

    abstract fun process(context: WorkerContext<Input, Output>): MincerResultType

}