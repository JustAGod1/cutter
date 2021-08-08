package ru.justagod.processing.cutter.base

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext

open class AugmentedMincer<Input, Output>() : SubMincer<Input, Output> {

    private val augments = arrayListOf<MincerAugment<Input, Output>>()

    constructor(augments: List<MincerAugment<Input, Output>>): this() {
        augments.forEach(this::register)
    }

    protected fun register(augment: MincerAugment<Input, Output>) {
        augments += augment
    }

    protected fun <T: MincerAugment<Input, Output>>T.register(): T {
        register(this)
        return this
    }

    override fun process(context: WorkerContext<Input, Output>): MincerResultType {
        var result = MincerResultType.SKIPPED

        for (i in augments.indices) {
            val augment = augments[i]

            val augmentResult = augment.process(context)

            if (augmentResult == MincerResultType.DELETED) return MincerResultType.DELETED
            if (augmentResult == MincerResultType.MODIFIED) result = MincerResultType.MODIFIED
        }

        return result
    }
}