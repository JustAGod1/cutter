package ru.justagod.plugin.test.test1

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.InheritanceHelper

class Test1Verifier(private val existedMethod: String, private val vanishedMethod: String): SubMincer<Unit, Unit> {
    private var classFounded = false
    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        if (!classFounded && context.name.name == "test1.Simple") {
            classFounded = true
            var methodFounded = false
            val node = context.info!!.node
            for (method in node.methods) {
                if (method.name == vanishedMethod) throw error("\"$vanishedMethod\" method has been found")
                if (method.name == existedMethod) methodFounded = true
            }
            if (!methodFounded) throw error("Can't find \"$existedMethod\"")
        }

        return MincerResultType.SKIPPED
    }

    override fun endProcessing(input: Unit, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, Unit>) {
        if (!classFounded) error("Can't find target class")
    }
}