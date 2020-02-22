package ru.justagod.plugin.test.test8

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.test.common.TestVerifierMincer

class Test8Verifier : TestVerifierMincer() {
    override fun mandatoryClasses(): Set<ClassTypeReference> = hashSetOf(
            ClassTypeReference("test8.Simple"),
            ClassTypeReference("test8.Simple$2")

    )

    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        assert(context.name.name != "test8.Simple$1")
        return MincerResultType.SKIPPED
    }
}