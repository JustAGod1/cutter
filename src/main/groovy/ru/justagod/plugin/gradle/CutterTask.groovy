package ru.justagod.plugin.gradle

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.zeroturnaround.zip.ZipEntrySource
import org.zeroturnaround.zip.ZipUtil
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.control.MincerControlPane
import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.util.MincerUtils
import ru.justagod.plugin.data.CutterConfig
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.processing.CutterPipelines
import ru.justagod.plugin.util.Extensions

class CutterTask extends DefaultTask {

    CutterConfig config
    CutterTaskData data

    @TaskAction
    void process() {
        println(data.invokeClasses)
        processArchive(project.tasks.findByPath("jar").getArchivePath(), data.name)
    }

    @CompileStatic
    private void processArchive(File f, String name) {
        def archive = MincerUtils.INSTANCE.processArchive(f) {
            this.buildMincer(it)
        }
        def target = new File(f.absoluteFile.parentFile, Extensions.nameWithoutExtension(f) + "-" + name + "." + Extensions.extension(f) ?: "")
        ZipUtil.pack((ZipEntrySource[]) (Object) archive.entries.values().toArray(), target)
    }



    @CompileStatic
    private MincerControlPane buildMincer(MincerFS fs) {
        def builder = new Mincer.Builder(fs, false)
        builder.registerSubMincer(
                CutterPipelines.INSTANCE.makePipeline(config.annotation, data)
        )
        builder.build()
    }

}
