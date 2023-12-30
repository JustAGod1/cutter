package ru.justagod.processing.cutter.analization

import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.base.MincerAugment
import ru.justagod.processing.cutter.config.SideName

abstract class AnalysisAugment(protected val config: CutterConfig) : MincerAugment<Unit, Unit>() {

    protected fun getSides(clazz: ClassNode): Set<SideName>? {
        return getSidesFromAnnotations(clazz.invisibleAnnotations)
            ?: getSidesFromAnnotations(clazz.visibleAnnotations)
    }

    protected fun getSides(field: FieldNode): Set<SideName>? {
        return getSidesFromAnnotations(field.invisibleAnnotations)
            ?: getSidesFromAnnotations(field.visibleAnnotations)
    }

    protected fun getSides(method: MethodNode): Set<SideName>? {
        return getSidesFromAnnotations(method.invisibleAnnotations)
            ?: getSidesFromAnnotations(method.visibleAnnotations)
    }

    private fun getSidesFromAnnotations(annotations: List<AnnotationNode>?): Set<SideName>? {
        annotations ?: return null

        return annotations.find { it.desc == config.annotation.desc() }?.let { getSidesFromAnnotation(it) }
    }

    private fun getSidesFromAnnotation(annotationNode: AnnotationNode): Set<SideName>? {
        return annotationNode.values?.let { extractSides(it) }?.toSet()
    }

    /**
     * Extracts list of sides from annotation value
     */
    private fun extractSides(entries: List<Any>): List<SideName>? {
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val name = iterator.next() as String
            val value = iterator.next()
            if (name == "value") {
                if (value is Array<*>) {
                    return listOf(SideName.make(value[1] as String))
                } else if (value is List<*>) {
                    if (value.isEmpty()) return emptyList()
                    if (value[0] !is Array<*>) return null
                    return value.map { SideName.make((it as Array<String>)[1]) }
                } else return null
            }
        }
        return null
    }

}