package ru.justagod.plugin.test.test1

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.plugin.test.common.TestVerifierMincer

class Test1Verifier(private val existedMethod: String, private val vanishedMethod: String): TestVerifierMincer() {
    private var classFounded = false
    override fun mandatoryClasses(): Set<ClassTypeReference> = hashSetOf(ClassTypeReference("test1.Simple"))

    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        if (!classFounded && context.name.name == "test1.Simple") {
            classFounded = true
            var methodFounded = false
            val node = context.info!!.node
            for (method in node.methods) {
                if (method.name == vanishedMethod) throw error("\"$vanishedMethod\" method has been found")
                if (method.name == existedMethod) methodFounded = true
            }
            assert(methodFounded)
        }

        return MincerResultType.SKIPPED
    }

}