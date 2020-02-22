package ru.justagod.plugin.test.common

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper

class ClassSearcherMincer(targets: Set<ClassTypeReference>): SubMincer<Unit, Unit> {

    private val notFound = targets.toMutableSet()

    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        notFound -= context.name
        return MincerResultType.SKIPPED
    }

    override fun endProcessing(input: Unit, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, Unit>) {
        assert(notFound.isEmpty())
    }
}