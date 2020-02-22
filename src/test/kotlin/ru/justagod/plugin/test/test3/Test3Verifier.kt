package ru.justagod.plugin.test.test3

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.plugin.test.common.TestVerifierMincer

class Test3Verifier(private val server: Boolean) : TestVerifierMincer() {
    override fun mandatoryClasses(): Set<ClassTypeReference> {
        if (server) {
            return hashSetOf(
                    ClassTypeReference("test3.server.package-info"),
                    ClassTypeReference("test3.server.Simple"),
                    ClassTypeReference("test3.server.Simple$1"),
                    ClassTypeReference("test3.server.Simple\$Simple1")
            )
        } else {
            return hashSetOf(
                    ClassTypeReference("test3.client.package-info"),
                    ClassTypeReference("test3.client.Simple"),
                    ClassTypeReference("test3.client.Simple$1"),
                    ClassTypeReference("test3.client.Simple\$Simple2")
            )
        }
    }

    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        if (server) {
            assert(!context.name.name.startsWith("test3.client"))
        } else {
            assert(!context.name.name.startsWith("test3.server"))
        }

        return MincerResultType.SKIPPED
    }


}