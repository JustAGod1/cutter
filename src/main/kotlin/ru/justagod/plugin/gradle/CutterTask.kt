package ru.justagod.plugin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import ru.justagod.cutter.mincer.util.MincerUtils
import ru.justagod.cutter.mincer.util.recursiveness.ByteArraySource
import ru.justagod.plugin.data.BakedCutterTaskData

open class CutterTask : DefaultTask() {
    lateinit var dataHarvester: () -> BakedCutterTaskData
    var archiveName: (() -> String?)? = null
    private val data: BakedCutterTaskData by lazy { dataHarvester() }

    @TaskAction
    fun process() {
        //processArchive((this.project.tasks.findByPath("jar") as AbstractArchiveTask?)!!.archivePath, data.name)
    }

    /*
    private fun processArchive(f: File, name: String) {
        val pipeline =
            if (CutterPlugin.instance.config.validation) makePipelineWithValidation(data) else makePipeline(data)
        val archive = MincerUtils.readZip(f)
        val librariesData = collectLibraries()
        val generalFs = MincerZipFS(archive)
        val librariesFs = MincerZipFS(librariesData)

        val router = MincerFallbackFS(generalFs, librariesFs)

        val mincer = Mincer.Builder(router)
            .registerPipeline(pipeline)
            .build()

        val resultEntries = hashMapOf<String, ByteArraySource>()
        do {
            val iterator = generalFs.entries.entries.iterator()
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
            generalFs.entries.putAll(resultEntries)
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
        val target = File(
            f.absoluteFile.parentFile, archiveName?.invoke()
                ?: f.nameWithoutExtension + "-" + name + "." + f.extension
        )
        ZipUtil.pack(generalFs.entries.values.toTypedArray(), target)
    }

     */

    private fun collectLibraries(): MutableMap<String, ByteArraySource> {
        val result = hashMapOf<String, ByteArraySource>()
        val javaConv = project.convention.plugins["java"] as JavaPluginConvention
        for (file in javaConv.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).compileClasspath.files) {
            if (file.extension == "jar" || file.extension == "zip") {
                result.putAll(MincerUtils.readZip(file))
            }
        }

        return result
    }
}