package ru.justagod.cutter.processing.transformation

import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.processor.WorkerContext
import ru.justagod.cutter.processing.base.MincerAugment
import ru.justagod.cutter.processing.transformation.validation.ValidationError

class HeuristicAugment : MincerAugment<Unit, List<ValidationError>>() {
    override fun process(context: WorkerContext<Unit, List<ValidationError>>): MincerResultType {
        return MincerResultType.SKIPPED
    }
}