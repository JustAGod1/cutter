package ru.justagod.plugin.test.test2

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.InheritanceHelper

class Test2Verifier(private val server: Boolean): SubMincer<Unit, Unit> {
    private val founded = hashSetOf<String>()
    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        if (!server && context.name.name.startsWith("test2.Simple1")) {
            error("${context.name} has been founded")
        } else if (server && context.name.name.startsWith("test2.Simple2")) {
            error("${context.name} has been founded")
        } else {
            founded += context.name.name
        }

        return MincerResultType.SKIPPED
    }

    override fun endProcessing(input: Unit, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, Unit>) {
        if (server) {
            assert("test2.Simple1" in founded)
            assert("test2.Simple1$1" in founded)
        } else {
            assert("test2.Simple2" in founded)
            assert("test2.Simple2$1" in founded)
        }
    }
}