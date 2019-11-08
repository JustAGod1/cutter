package ru.justagod.plugin.test.test7

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext

class Test7Verifier(private val server: Boolean) : SubMincer<Unit, Unit> {
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
        }

        return MincerResultType.SKIPPED
    }
}