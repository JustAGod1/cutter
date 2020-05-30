package ru.justagod.plugin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.zeroturnaround.zip.ZipUtil
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.util.MincerUtils
import ru.justagod.mincer.util.recursiveness.ByteArraySource
import ru.justagod.mincer.util.recursiveness.MincerZipFS
import ru.justagod.plugin.data.BakedCutterTaskData
import ru.justagod.plugin.processing.CutterPipelines.makePipeline
import ru.justagod.plugin.processing.CutterPipelines.makePipelineWithValidation
import ru.justagod.plugin.processing.pipeline.validation.ValidationResult
import java.io.File

open class CutterTask : DefaultTask() {
    lateinit var dataHarvester: () -> BakedCutterTaskData
    var archiveName: (() -> String?)? = null
    private val data: BakedCutterTaskData by lazy { dataHarvester() }

    @TaskAction
    fun process() {
        processArchive((this.project.tasks.findByPath("jar") as AbstractArchiveTask?)!!.archivePath, data.name)
    }

    private fun processArchive(f: File, name: String) {
        val pipeline = if (CutterPlugin.instance.config.validation) makePipelineWithValidation(data) else makePipeline(data)
        val archive = MincerUtils.readZip(f)
        val fs = MincerZipFS(f, archive)

        val mincer = Mincer.Builder(fs, false)
                .registerSubMincer(pipeline)
                .build()

        val resultEntries = hashMapOf<String, ByteArraySource>()
        do {
            val iterator = fs.entries.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (!entry.value.path.endsWith(".class")) {
                    resultEntries[entry.key] = entry.value
                    continue
                }
                if (data.excludes(entry.value.path)) {
                    resultEntries[entry.key] = entry.value
                    continue
                }

                val result = mincer.advance(entry.value.bytes, entry.value.path, 0)
                if (result.type == MincerResultType.DELETED) {
                    iterator.remove()
                    resultEntries -= entry.key
                } else {
                    resultEntries[entry.key] = ByteArraySource(entry.key, result.resultedBytecode)
                }
            }
            archive.entries.clear()
            fs.entries.putAll(resultEntries)
        } while (mincer.endIteration())

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
        val target = File(f.absoluteFile.parentFile, archiveName?.invoke()
                ?: f.nameWithoutExtension + "-" + name + "." + f.extension)
        ZipUtil.pack(fs.entries.values.toTypedArray(), target)
    }

    private fun processArchive() {


    }
}