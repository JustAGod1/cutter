package ru.justagod.cutter.processing.base

import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.processor.SubMincer
import ru.justagod.cutter.mincer.processor.WorkerContext

/**
 * It's like moduled sub mincer.
 *
 * This sub mincer has modules called augments. It just calls them one by one.
 * That's it
 */
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