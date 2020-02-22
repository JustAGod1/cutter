package ru.justagod.plugin.test.test4

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.test.common.TestVerifierMincer

class Test4Verifier(private val server: Boolean): TestVerifierMincer() {
    override fun mandatoryClasses(): Set<ClassTypeReference> = hashSetOf(ClassTypeReference("test4.Class2"))

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