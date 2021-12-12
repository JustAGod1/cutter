package ru.justagod.plugin.gradle

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.DefaultZipCompressor
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import ru.justagod.cutter.processing.config.CutterConfig
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.Callable

@Suppress("UnstableApiUsage")
@CacheableTask
open class CutterTask : Jar() {

    @InputFiles @PathSensitive(PathSensitivity.RELATIVE)
    val classPath: ListProperty<FileCollection> = project.objects.listProperty(FileCollection::class.java)
    @Input
    val config: Property<CutterConfig> = project.objects.property(CutterConfig::class.java)
    @Input @Optional
    val threadsCount: Property<Int> = project.objects.property(Int::class.java)

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getUsedFiles(): ConfigurableFileCollection {
        return project.files(Callable {
            val configurations = classPath.get()
            val result = hashSetOf<File>()
            configurations.flatMapTo(result) { it }
            return@Callable result
        })
    }

    override fun createCopyAction(): CopyAction {
        val cacheDir = project.file(".gradle").resolve("cutter-transition")
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        return CutterCopyAction(
            cacheDir = cacheDir,
            targetFile = destinationDir.resolve(archiveName),
            processor = DefaultCutterProcessor(threadsCount.getOrElse(10), config.get(), classPath.get()),
            encoding = metadataCharset ?: Charset.defaultCharset().name()
        )
    }
}