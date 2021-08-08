package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.AnnotationNode
import ru.justagod.cutter.mincer.MincerBuilder
import ru.justagod.cutter.mincer.control.MincerArchive
import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.filter.WalkThroughFilter
import ru.justagod.cutter.mincer.pipeline.Pipeline
import ru.justagod.cutter.mincer.processor.SubMincer
import ru.justagod.cutter.mincer.processor.WorkerContext
import ru.justagod.cutter.mincer.util.MincerDecentFS
import ru.justagod.cutter.mincer.util.MincerUtils
import ru.justagod.cutter.mincer.util.makeFirstSimple
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.model.InheritanceHelper
import ru.justagod.plugin.data.BakedCutterTaskData
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.test.base.context.GradleContext
import ru.justagod.plugin.test.base.context.StraightContext
import java.io.File

object AnnotationTests {

    private const val annotation = "ru.justagod.cutter.GradleSideOnly"

    @Test
    fun gradle() {
        val script = """
        cutter {
                annotation = "$annotation"
                removeAnnotations = true
                def serverSide = side('SERVER')
                def clientSide = side('CLIENT')
                builds {
                    client {
                        targetSides = [clientSide]
                        primalSides = [clientSide, serverSide]
                    }
                    server {
                        targetSides = [serverSide]
                        primalSides = [clientSide, serverSide]
                    }
                }
            }
    """.trimIndent()
        val context = GradleContext(script)
        context.before()
        val virgin = context.compileResourceFolder("test8", null)
        assert(!validate(virgin))

        val defiled = context.compileResourceFolder("test8", "server")
        assert(validate(defiled))
    }

    @Test
    fun straight() {
        val context = StraightContext { name ->
            BakedCutterTaskData(
                    name = name,
                    annotation = ClassTypeReference(annotation),
                    validationOverrideAnnotation = null,
                    removeAnnotations = true,
                    primalSides = setOf(SideName.make("server"), SideName.make("client")),
                    targetSides = setOf(SideName.make(name)),
                    invocators = emptyList(),
                    markers = emptyList(),
                    excludes = { false }
            )
        }
        val virgin = context.compileResourceFolder("test8", null)
        assert(!validate(virgin))

        val defiled = context.compileResourceFolder("test8", "server")
        assert(validate(defiled))
    }

    private fun validate(compiled: File): Boolean {
        val pipeline = Pipeline.makeFirstSimple(AnnotationsSearcher(annotation), WalkThroughFilter, null)
        val mincer = MincerBuilder(MincerDecentFS(compiled), false)
                .registerSubMincer(pipeline)
                .build()
        MincerUtils.processFolder(mincer, compiled)

        return pipeline.value!!
    }

    class AnnotationsSearcher(annotationName: String) : SubMincer<Unit, Boolean> {
        private val annotationDescriptor = 'L' + annotationName.replace('.', '/') + ';'
        override fun process(context: WorkerContext<Unit, Boolean>): MincerResultType {
            val node = context.info!!.node

            if (context.pipeline.value == false) return MincerResultType.SKIPPED

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

        private fun analyze(annotations: List<AnnotationNode>?, pipeline: Pipeline<Unit, Boolean>): Boolean {
            if (annotations == null) return false
            if (pipeline.value == false) return true
            if (annotations.any { it.desc == annotationDescriptor }) {
                pipeline.value = false
                return true
            }
            return false
        }

        override fun endProcessing(input: Unit, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, Boolean>) {
            if (pipeline.value != false) pipeline.value = true
        }

    }
}