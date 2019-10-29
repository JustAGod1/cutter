package ru.justagod.plugin.processing

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext

class ModelBuilderMincer: SubMincer<Unit, ProjectModel> {
    override fun process(context: WorkerContext<Unit, ProjectModel>): MincerResultType {

        return MincerResultType.SKIPPED
    }
}