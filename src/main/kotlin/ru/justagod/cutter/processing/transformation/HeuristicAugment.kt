package ru.justagod.cutter.processing.cutter.transformation

import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.processor.WorkerContext
import ru.justagod.cutter.processing.cutter.base.MincerAugment
import ru.justagod.cutter.processing.cutter.transformation.validation.ValidationError

class HeuristicAugment : MincerAugment<Unit, List<ValidationError>>() {
    override fun process(context: WorkerContext<Unit, List<ValidationError>>): MincerResultType {
        return MincerResultType.SKIPPED
    }
}