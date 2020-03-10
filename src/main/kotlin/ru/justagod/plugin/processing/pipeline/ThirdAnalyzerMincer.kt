package ru.justagod.plugin.processing.pipeline

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.InheritanceHelper
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.model.PathHelper
import ru.justagod.plugin.processing.model.ProjectModel
import ru.justagod.plugin.util.intersectsWith

class ThirdAnalyzerMincer(private val primalSides: Set<SideName>)
    : SubMincer<ProjectModel, ProjectModel> {

    override fun process(context: WorkerContext<ProjectModel, ProjectModel>): MincerResultType {
        if (context.name.simpleName == "package-info") return MincerResultType.SKIPPED
        val info = context.info!!
        val tree = context.input.sidesTree
        info.node.methods?.forEach { method ->
            val name = method.name + method.desc
            val sides = tree.get(context.name.path + name, primalSides).toMutableSet()

            context.mincer.inheritance.walk(context.name) {
                sides.intersectsWith(tree.get(PathHelper.method(it.name, method.name, method.desc), primalSides))
            }
            tree.set(PathHelper.method(context.name, method.name, method.desc), sides)
        }

        return MincerResultType.SKIPPED
    }

    override fun endProcessing(input: ProjectModel, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<ProjectModel, ProjectModel>) {
        input.sidesTree.identify(null)

        pipeline.value = input
    }

}