package ru.justagod.mincer.pipeline

import ru.justagod.mincer.filter.ClassFilter
import ru.justagod.mincer.processor.SubMincer

class Pipeline<in Input : Any, Output : Any> private constructor(
        val id: String,
        val worker: SubMincer<in Input, Output>,
        internal val filter: ClassFilter,
        internal val parent: Pipeline<*, in Input>? = null,
        default: Output?,
        internal val skippable: Boolean = true,
        internal val cacheClearingRequired: Boolean = false
) {

    var value: Output? = default

    companion object {

        @JvmOverloads
        @JvmStatic
        fun <Input : Any, Output : Any> make(
                id: String,
                worker: SubMincer<in Input, Output>,
                filter: ClassFilter,
                parent: Pipeline<*, out Input>,
                default: Output? = null,
                skippable: Boolean = true,
                cacheCleaningRequired: Boolean = false
        ) = Pipeline(id, worker, filter, parent, default, skippable, cacheCleaningRequired)

        @JvmOverloads
        @JvmStatic
        fun <Output : Any> makeFirst(
                id: String,
                worker: SubMincer<Unit, Output>,
                filter: ClassFilter,
                skippable: Boolean = true,
                default: Output? = null,
                cacheCleaningRequired: Boolean = false
        ): Pipeline<Unit, Output> = Pipeline(id, worker, filter, null, default, skippable, cacheCleaningRequired)
    }
}