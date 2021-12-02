package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.AnnotationNode
import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.pipeline.MincerPipeline
import ru.justagod.cutter.mincer.processor.SubMincer
import ru.justagod.cutter.mincer.processor.WorkerContext
import ru.justagod.cutter.mincer.util.MincerDecentFS
import ru.justagod.cutter.mincer.util.MincerUtils
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.processing.config.CutterConfig
import ru.justagod.cutter.processing.config.SideName
import ru.justagod.plugin.test.base.context.GradleContext
import ru.justagod.plugin.test.base.context.StraightContext
import java.io.File

object AnnotationTests {

    private const val annotation = "ru.justagod.cutter.GradleSideOnly"

    @Test
    fun enabled() {
        val context = StraightContext { name ->
            CutterConfig(
                annotation = ClassTypeReference(annotation),
                validationOverrideAnnotation = null,
                primalSides = setOf(SideName.make("server"), SideName.make("client")),
                targetSides = setOf(SideName.make(name)),
                invocators = emptyList(),
                deleteAnnotations = true
            )
        }
        val virgin = context.compileResourceFolder("test8", null)
        assert(!validate(virgin))

        val defiled = context.compileResourceFolder("test8", "server")
        assert(validate(defiled))
    }

    @Test
    fun disabled() {
        val context = StraightContext { name ->
            CutterConfig(
                annotation = ClassTypeReference(annotation),
                validationOverrideAnnotation = null,
                primalSides = setOf(SideName.make("server"), SideName.make("client")),
                targetSides = setOf(SideName.make(name)),
                invocators = emptyList(),
                deleteAnnotations = false
            )
        }
        val virgin = context.compileResourceFolder("test8", null)
        assert(!validate(virgin))

        val defiled = context.compileResourceFolder("test8", "server")
        assert(!validate(defiled))
    }

    private fun validate(compiled: File): Boolean {
        val pipeline = MincerPipeline.make(AnnotationsSearcher(annotation), true).build()
        val mincer = Mincer.Builder(MincerDecentFS(compiled))
            .registerPipeline(pipeline)
            .build()
        MincerUtils.processFolder(mincer, compiled, threadsCount = 1)

        return pipeline.result()
    }

    class AnnotationsSearcher(annotationName: String) : SubMincer<Unit, Boolean> {
        private val annotationDescriptor = 'L' + annotationName.replace('.', '/') + ';'
        override fun process(context: WorkerContext<Unit, Boolean>): MincerResultType {
            val node = context.info.node()

            if (!context.pipeline.output) return MincerResultType.SKIPPED

            if (analyze(node.invisibleAnnotations, context.pipeline)) return MincerResultType.SKIPPED
            if (analyze(node.visibleAnnotations, context.pipeline)) return MincerResultType.SKIPPED
            node.fields?.forEach {
                if (analyze(it.visibleAnnotations, context.pipeline)) return MincerResultType.SKIPPED
                if (analyze(it.invisibleAnnotations, context.pipeline)) return MincerResultType.SKIPPED
            }
            node.methods?.forEach {
                if (analyze(it.visibleAnnotations, context.pipeline)) return MincerResultType.SKIPPED
                if (analyze(it.invisibleAnnotations, context.pipeline)) return MincerResultType.SKIPPED
            }

            return MincerResultType.SKIPPED
        }

        private fun analyze(annotations: List<AnnotationNode>?, pipeline: MincerPipeline<Unit, Boolean>): Boolean {
            if (annotations == null) return false
            if (!pipeline.output) return true
            if (annotations.any { it.desc == annotationDescriptor }) {
                pipeline.output = false
                return true
            }
            return false
        }

    }
}