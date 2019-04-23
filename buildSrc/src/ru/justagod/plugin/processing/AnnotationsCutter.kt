package ru.justagod.plugin.processing

import org.objectweb.asm.tree.ClassNode
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.ProcessingResult
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory

class AnnotationsCutter(private val annotation: ClassTypeReference): SubMincer<Any, Unit> {
    override fun process(
            name: ClassTypeReference,
            data: ClassModel?,
            node: ClassNode?,
            pipeline: Pipeline<Any, Unit>,
            input: Any,
            inheritance: InheritanceHelper,
            nodes: NodesFactory,
            factory: ModelFactory,
            skipped: Boolean
    ): ProcessingResult {
        data!!
        node!!

        val annotationDesc = "L" + annotation.internalName + ";"
        var changed = false
        if (node.invisibleAnnotations?.removeIf { it.desc == annotationDesc } == true) changed = true
        if (node.visibleAnnotations?.removeIf { it.desc == annotationDesc } == true) changed = true
        node.methods?.forEach {
            if (it?.invisibleAnnotations?.removeIf { it.desc == annotationDesc } == true) changed = true
            if (it?.visibleAnnotations?.removeIf { it.desc == annotationDesc } == true) changed = true
        }
        node.fields?.forEach {
            if (it?.invisibleAnnotations?.removeIf { it.desc == annotationDesc } == true) changed = true
            if (it?.visibleAnnotations?.removeIf { it.desc == annotationDesc } == true) changed = true
        }

        return if (changed) ProcessingResult.REWRITE else ProcessingResult.NOOP
    }
}