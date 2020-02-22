package ru.justagod.plugin.test.test2

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.plugin.test.common.TestVerifierMincer

class Test2Verifier(private val server: Boolean): TestVerifierMincer() {
    private val founded = hashSetOf<String>()
    override fun mandatoryClasses(): Set<ClassTypeReference> {
        if (server) {
            return hashSetOf(
                    ClassTypeReference("test2.Simple1"),
                    ClassTypeReference("test2.Simple1$1")
            )
        } else {
            return hashSetOf(
                    ClassTypeReference("test2.Simple2"),
                    ClassTypeReference("test2.Simple2$1")
            )
        }
    }

    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        if (!server && context.name.name.startsWith("test2.Simple1")) {
            error("${context.name} has been founded")
        } else if (server && context.name.name.startsWith("test2.Simple2")) {
            error("${context.name} has been founded")
        }

        return MincerResultType.SKIPPED
    }

}