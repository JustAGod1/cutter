package ru.justagod.plugin.processing.pipeline

import org.objectweb.asm.tree.AnnotationNode
import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.InheritanceHelper
import ru.justagod.plugin.processing.model.ProjectModel

class AnnotationsRemoverMincer(annotationName: String) : SubMincer<ProjectModel, ProjectModel>{
    private val annotationDescriptor = 'L' + annotationName.replace('.', '/') + ';'

    override fun process(context: WorkerContext<ProjectModel, ProjectModel>): MincerResultType {
        val node = context.info!!.node
        var modified = false

        modified = modified or considerAndProcess(node.invisibleAnnotations)
        modified = modified or considerAndProcess(node.visibleAnnotations)
        node.fields?.forEach {
            modified = modified or considerAndProcess(it.invisibleAnnotations)
            modified = modified or considerAndProcess(it.visibleAnnotations)
        }
        node.methods?.forEach {
            modified = modified or considerAndProcess(it.invisibleAnnotations)
            modified = modified or considerAndProcess(it.visibleAnnotations)
        }

        return if (modified) MincerResultType.MODIFIED else MincerResultType.SKIPPED
    }


    private fun considerAndProcess(annotations: MutableList<AnnotationNode>?): Boolean {
        if (annotations == null) return false
        var result = false
        val iter = annotations.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (v.desc == annotationDescriptor) {
                iter.remove()
                result = true
            }
        }

        return result
    }

    override fun endProcessing(input: ProjectModel, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<ProjectModel, ProjectModel>) {
        pipeline.value = input
    }
}