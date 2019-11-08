package ru.justagod.plugin.test.test4

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext

class Test4Verifier(private val server: Boolean): SubMincer<Unit, Unit> {

    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        if (context.name.name == "test4.Class2") {
            val node = context.info!!.node
            if (server) {
                assert(node.methods.find { it.name == "server" && it.desc == "()V" } != null)
                assert(node.methods.find { it.name == "server" && it.desc == "(I)V" } != null)

                assert(node.methods.find { it.name == "client" && it.desc == "()V" } == null)
                assert(node.methods.find { it.name == "client" && it.desc == "(I)V" } != null)
            } else {
                assert(node.methods.find { it.name == "server" && it.desc == "()V" } == null)
                assert(node.methods.find { it.name == "server" && it.desc == "(I)V" } != null)

                assert(node.methods.find { it.name == "client" && it.desc == "()V" } != null)
                assert(node.methods.find { it.name == "client" && it.desc == "(I)V" } != null)
            }
        }
        return MincerResultType.SKIPPED
    }
}