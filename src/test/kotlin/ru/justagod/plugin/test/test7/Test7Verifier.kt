package ru.justagod.plugin.test.test7

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.plugin.test.common.TestVerifierMincer

class Test7Verifier(private val server: Boolean) : TestVerifierMincer() {
    private var done = false
    override fun mandatoryClasses(): Set<ClassTypeReference> = hashSetOf(ClassTypeReference("test7.Simple"))

    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        if (context.name.name == "test7.Simple") {
            if (server) {
                context.info!!.node.fields.forEach {
                    assert(it.name == "server")
                }
            } else {
                context.info!!.node.fields.forEach {
                    assert(it.name == "client")
                }
            }
            done = true
        }

        return MincerResultType.SKIPPED
    }

    override fun endProcessing(input: Unit, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, Unit>) {
        assert(done)
    }

}