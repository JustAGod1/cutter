package ru.justagod.plugin.test.test5

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext

class Test5Verifier : SubMincer<Unit, Unit> {
    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        assert(context.name.name != "test5.Simple$1")
        if (context.name.name == "test5.Simple") {
            assert(context.info!!.node.methods.find { it.name == "server" && it.desc == "()V" } == null)
        }
        return MincerResultType.SKIPPED
    }
}