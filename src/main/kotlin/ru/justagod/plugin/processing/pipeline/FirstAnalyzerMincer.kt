package ru.justagod.plugin.processing.pipeline

import org.objectweb.asm.tree.AnnotationNode
import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.InheritanceHelper
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.model.PathHelper
import ru.justagod.plugin.processing.model.ProjectModel

class FirstAnalyzerMincer(annotationName: String): SubMincer<Unit, ProjectModel> {

    private val annotationDescriptor = 'L' + annotationName.replace('.', '/') + ';'

    override fun process(context: WorkerContext<Unit, ProjectModel>): MincerResultType {
        val node = context.info!!.node
        val project = context.pipeline.value!!

        inscribeSides(node.invisibleAnnotations, PathHelper.klass(context.name), project)
        inscribeSides(node.visibleAnnotations, PathHelper.klass(context.name), project)

        node.methods?.forEach {
            val name = PathHelper.method(context.name, it.name, it.desc)
            inscribeSides(it.invisibleAnnotations, name, project)
            inscribeSides(it.visibleAnnotations, name, project)
        }
        node.fields?.forEach {
            val name = PathHelper.field(context.name, it.name, it.desc)
            inscribeSides(it.invisibleAnnotations, name, project)
            inscribeSides(it.visibleAnnotations, name, project)
        }

        return MincerResultType.SKIPPED
    }

    private fun inscribeSides(annotations: List<AnnotationNode>?, name: List<String>, model: ProjectModel) {
        if (annotations == null) return

        for (node in annotations) {
            if (node.desc != annotationDescriptor) continue
            val sides = extractSides(node.values)?.toSet() ?: continue
            model.sidesTree.set(name, sides)
        }
    }

    private fun extractSides(entries: List<Any>): List<SideName>? {
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val name = iterator.next() as String
            val value = iterator.next()
            if (name == "value") {
                if (value is Array<*>) {
                    return listOf(SideName.make(value[1] as String))
                } else if (value is List<*>) {
                    if (value[0] !is Array<*>) return null
                    return value.map { SideName.make((it as Array<String>)[1]) }
                } else return null
            }
        }
        return null
    }


    override fun endProcessing(input: Unit, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, ProjectModel>) {
        pipeline.value!!.sidesTree.identify(null)
    }
}