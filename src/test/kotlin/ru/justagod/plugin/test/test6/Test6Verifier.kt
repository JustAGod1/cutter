package ru.justagod.plugin.test.test6

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext

class Test6Verifier: SubMincer<Unit, Unit> {
    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        if (context.name.name == "test6.Simple") {
            assert(context.info!!.node.methods.size == 1)
            assert(context.info!!.node.methods[0].name == "<init>")
        }
        return MincerResultType.SKIPPED
    }
}