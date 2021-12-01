package ru.justagod.cutter.processing.analization

import ru.justagod.cutter.processing.config.CutterConfig
import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.processor.WorkerContext
import ru.justagod.cutter.processing.model.*

class MembersMarkerAugment(private val model: ProjectModel, config: CutterConfig) : AnalysisAugment(config) {
    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        if (context.name.simpleName == "package-info") {
            model.atom(FolderAtom(context.name.name.dropLast(13)), getSides(context.info.node()))
            return MincerResultType.SKIPPED
        }
        val root = ClassAtom(context.name)
        model.atom(root, getSides(context.info.node()))

        context.info.node().methods?.forEach {
            val atom = MethodAtom(context.name, it.name, it.desc)

            model.atom(atom, getSides(it))
            model.join(atom, root)
        }

        context.info.node().fields?.forEach {
            val atom = FieldAtom(context.name, it.name)

            model.atom(atom, getSides(it))
            model.join(atom, root)
        }

        return MincerResultType.SKIPPED
    }
}