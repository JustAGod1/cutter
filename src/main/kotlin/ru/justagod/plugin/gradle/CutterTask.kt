package ru.justagod.plugin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.zeroturnaround.zip.ZipEntrySource
import org.zeroturnaround.zip.ZipUtil
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.util.MincerUtils.processArchive
import ru.justagod.plugin.data.BakedCutterTaskData
import ru.justagod.plugin.data.CutterConfig
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.CutterPipelines.makePipeline
import ru.justagod.plugin.processing.CutterPipelines.makePipelineWithValidation
import ru.justagod.plugin.processing.pipeline.validation.ValidationResult
import ru.justagod.plugin.processing.pipeline.validation.data.ValidationError
import java.io.File
import javax.inject.Inject

open class CutterTask: DefaultTask() {
    lateinit var dataHarvester: () -> BakedCutterTaskData
    var archiveName: (() -> String?)? = null
    private val data: BakedCutterTaskData by lazy { dataHarvester() }

    @TaskAction
    fun process() {
        processArchive((this.project.tasks.findByPath("jar") as AbstractArchiveTask?)!!.archivePath, data.name)
    }

    private fun processArchive(f: File, name: String) {
        val pipeline = if (CutterPlugin.instance.config.validation) makePipelineWithValidation(data) else makePipeline(data)
        val archive = processArchive(f
        ) {
            Mincer.Builder(it, false)
                    .registerSubMincer(pipeline)
                    .build()
        }
        if (CutterPlugin.instance.config.validation) {
            val value = pipeline.value as ValidationResult
            if (value.isNotEmpty()) {
                for ((side, value1) in value) {
                    System.err.println("Validation errors for side ${side.name}:")
                    for (err in value1) {
                        System.err.println(err)
                    }
                }
                throw RuntimeException("Validation failed")
            }
        }
        val target = File(f.absoluteFile.parentFile, archiveName?.invoke() ?: f.nameWithoutExtension + "-" + name + "." + f.extension)
        ZipUtil.pack(archive.entries.values.toTypedArray(), target)
    }
}