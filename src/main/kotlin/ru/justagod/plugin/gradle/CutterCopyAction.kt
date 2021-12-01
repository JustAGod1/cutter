package ru.justagod.plugin.gradle

import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.copy.*
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.WorkResult
import org.gradle.workers.internal.DefaultWorkResult
import ru.justagod.cutter.processing.config.CutterConfig
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CutterCopyAction(
    private val cacheDir: File,
    private val targetFile: File,
    private val processor: CutterProcessor,
    private val encoding: String
) : CopyAction {
    override fun execute(stream: CopyActionProcessingStream): WorkResult {
        stream.process(ToCacheAction())
        processFolder()
        zipCache()

        return WorkResult { true }
    }

    private fun zipCache() {

        val fileOutput = ZipOutputStream(targetFile.outputStream())

        for (file in cacheDir.walkTopDown()) {
            if (file.isDirectory) continue

            val entry = ZipEntry(file.relativeTo(cacheDir).path)
            fileOutput.putNextEntry(entry)

            file.inputStream().copyTo(fileOutput)
            fileOutput.closeEntry()
        }
        fileOutput.close()
    }

    private fun processFolder() {
        processor.process(cacheDir)
    }

    private inner class ToCacheAction : CopyActionProcessingStreamAction {
        override fun processFile(details: FileCopyDetailsInternal) {
            if (details.isDirectory) return
            val targetFile = cacheDir.resolve(details.relativePath.pathString)

            details.file.copyTo(targetFile, overwrite = true)
        }

    }
}