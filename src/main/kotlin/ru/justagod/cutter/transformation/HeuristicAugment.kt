package ru.justagod.processing.cutter.transformation

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.processing.cutter.base.MincerAugment
import ru.justagod.processing.cutter.transformation.validation.ValidationError

class HeuristicAugment : MincerAugment<Unit, List<ValidationError>>() {
    override fun process(context: WorkerContext<Unit, List<ValidationError>>): MincerResultType {
        return MincerResultType.SKIPPED
    }
}