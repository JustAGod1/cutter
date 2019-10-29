package ru.justagod.plugin.gradle

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.control.MincerControlPane
import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.filter.WalkThroughFilter
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.util.MincerUtils
import ru.justagod.plugin.data.CutterConfig
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.processing.ModelBuilderMincer
import ru.justagod.plugin.processing.ProjectModel

@CompileStatic
class CutterTask extends DefaultTask {

    CutterConfig config
    CutterTaskData data

    @TaskAction
    void process(Project project) {

    }

    static void processArchive(File f, String name) {
        def archive = MincerUtils.INSTANCE.processArchive(f, CutterTask.&buildMincer)
        new File(f.absoluteFile.parentFile, f.nameWithoutExtension() + "-" + name)
    }



    private static MincerControlPane buildMincer(MincerFS fs) {
        def builder = new Mincer.Builder(fs, false)
        builder.registerSubMincer(makeCutterPipeline())
        builder.build()
    }

    private static Pipeline<?, ?> makeCutterPipeline() {
        return Pipeline.makeFirst(
                "model",
                new ModelBuilderMincer(),
                WalkThroughFilter.INSTANCE,
                new ProjectModel(),
                false
        )
    }
}
