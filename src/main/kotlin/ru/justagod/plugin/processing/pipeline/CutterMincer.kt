package ru.justagod.plugin.processing.pipeline

import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.ProjectModel
import ru.justagod.plugin.util.intersectsWith

class CutterMincer(private val targetSides: List<SideName>, private val primalSides: Set<SideName>): SubMincer<ProjectModel, Unit> {
    override fun process(context: WorkerContext<ProjectModel, Unit>): MincerResultType {
        val tree = context.input.sidesTree
        if (!tree.get(context.name.path, primalSides).intersectsWith(targetSides)) return MincerResultType.DELETED
        var modified = false
        val node = context.info!!.node
        val path = context.name.path

        if (node.fields != null) {
            val fieldsIter = node.fields.iterator()
            while (fieldsIter.hasNext()) {
                val field = fieldsIter.next()
                if (!tree.get(path + (field.name + field.desc), primalSides).intersectsWith(targetSides)) {
                    fieldsIter.remove()
                    modified = true
                }
            }
        }

        if (node.methods != null) {
            val methodsIter = node.methods.iterator()
            while (methodsIter.hasNext()) {
                val method = methodsIter.next()
                if (!tree.get(path + (method.name + method.desc), primalSides).intersectsWith(targetSides)) {
                    methodsIter.remove()
                    modified = true
                }
            }
        }

        return if (modified) MincerResultType.MODIFIED else MincerResultType.SKIPPED
    }


}