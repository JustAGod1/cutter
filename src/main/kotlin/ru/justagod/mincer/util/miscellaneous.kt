package ru.justagod.mincer.util

import ru.justagod.mincer.control.MincerControlPane
import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.filter.ClassFilter
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer

typealias MincerFactory = (MincerFS) -> MincerControlPane

private var counter = 0
fun <Output : Any> Pipeline.Companion.makeFirstSimple(
        worker: SubMincer<Unit, Output>,
        filter: ClassFilter,
        default: Output?
): Pipeline<Unit, Output> {
    counter++
    return Pipeline.makeFirst(
            counter.toString(),
            worker,
            filter,
            default,
            false
    )
}

fun <Input : Any, Output : Any> Pipeline.Companion.makeSimple(
        worker: SubMincer<in Input, Output>,
        parent: Pipeline<*, out Input>,
        filter: ClassFilter,
        default: Output?
) = Pipeline.make(
        (++counter).toString(),
        worker,
        filter,
        parent,
        default,
        false
)

fun <Input : Any, Output : Any> (Pipeline<*, out Input>).join(other: SubMincer<in Input, Output>, filter: ClassFilter, default: Output?) =
        Pipeline.makeSimple(
                other,
                this,
                filter,
                default
        )

