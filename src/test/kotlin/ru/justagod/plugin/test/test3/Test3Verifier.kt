package ru.justagod.plugin.test.test3

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.InheritanceHelper

class Test3Verifier(private val server: Boolean) : SubMincer<Unit, Unit> {
    private val founded = hashSetOf<String>()
    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        founded += context.name.name

        if (server) {
            assert(!context.name.name.startsWith("test3.client"))
        } else {
            assert(!context.name.name.startsWith("test3.server"))
        }

        return MincerResultType.SKIPPED
    }

    override fun endProcessing(input: Unit, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, Unit>) {
        if (server) {
            assert("test3.server.package-info" in founded)
            assert("test3.server.Simple" in founded)
            assert("test3.server.Simple$1" in founded)
            assert("test3.server.Simple\$Simple1" in founded)
        } else {
            assert("test3.client.package-info" in founded)
            assert("test3.client.Simple" in founded)
            assert("test3.client.Simple$1" in founded)
            assert("test3.client.Simple\$Simple2" in founded)
        }
    }

}