package ru.justagod.plugin.test.test5

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.test.common.TestVerifierMincer

class Test5Verifier : TestVerifierMincer() {
    override fun mandatoryClasses(): Set<ClassTypeReference> = hashSetOf(ClassTypeReference("test5.Simple"))

    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        assert(context.name.name != "test5.Simple$1")
        if (context.name.name == "test5.Simple") {
            assert(context.info!!.node.methods.find { it.name == "server" && it.desc == "()V" } == null)
        }
        return MincerResultType.SKIPPED
    }
}