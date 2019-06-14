package ru.justagod.plugin

import kotlin.Unit
import org.gradle.api.Project
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.filter.NoOpFilter
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.processing.AnnotationsCutter
import ru.justagod.plugin.processing.CutterMincer
import ru.justagod.plugin.processing.ModelBuilderMincer

import static ru.justagod.mincer.pipeline.Pipeline.makeFirst
import static ru.justagod.mincer.pipeline.Pipeline.make

class CutterAction {

    final String annotation
    final List<File> classesDirs
    final CutterTaskData data
    final File classesCache
    final Project project
    final boolean printSidesTree
    final boolean processDependencies
    final boolean deleteAnnotations

    CutterAction(
            String annotation,
            List<File> classesDirs,
            CutterTaskData data,
            File classesCache,
            Project project,
            boolean printSidesTree,
            boolean processDependencies,
            boolean deleteAnnotations
    ) {
        this.annotation = annotation
        this.classesDirs = classesDirs
        this.data = data
        this.classesCache = classesCache
        this.project = project
        this.printSidesTree = printSidesTree
        this.processDependencies = processDependencies
        this.deleteAnnotations = deleteAnnotations
    }

    def action() {
        classesCache.deleteDir()
        classesCache.mkdirs()
        project.copy {
            if (processDependencies) {
                def spec = project.jar.getMainSpec()
                spec.getSourcePaths().forEach { from(it) }
            }
            classesDirs.forEach {
                from(it)
            }
            into(classesCache)
        }
        def firstPass = new ModelBuilderMincer(new ClassTypeReference(annotation), data.primalSides, printSidesTree)
        def secondPass = new CutterMincer(data.targetSides, new HashSet(data.primalSides))
        def pipeline = make(
                "final",
                secondPass,
                NoOpFilter.INSTANCE,
                makeFirst("initial", firstPass, NoOpFilter.INSTANCE, false),
                Unit.INSTANCE
        )
        if (deleteAnnotations) pipeline = make(
                "cleaner",
                new AnnotationsCutter(new ClassTypeReference(annotation)),
                NoOpFilter.INSTANCE,
                pipeline,
                Unit.INSTANCE,
                false,
                true
        )
        def mincer = new Mincer.Builder(classesCache, []).registerSubMincer(pipeline).build()
        mincer.process(false)
    }
}
