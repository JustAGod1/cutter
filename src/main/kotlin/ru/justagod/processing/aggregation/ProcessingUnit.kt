package ru.justagod.processing.aggregation

import ru.justagod.mincer.pipeline.MincerPipelineController

abstract class ProcessingUnit<Argument> {

    abstract fun makePipelines(input: Argument): List<MincerPipelineController<*>>


}