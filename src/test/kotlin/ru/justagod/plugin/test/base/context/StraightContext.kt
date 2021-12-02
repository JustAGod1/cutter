package ru.justagod.plugin.test.base.context

import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.mincer.util.MincerDecentFS
import ru.justagod.cutter.mincer.util.MincerUtils
import ru.justagod.cutter.processing.CutterProcessingUnit
import ru.justagod.cutter.processing.config.CutterConfig
import ru.justagod.plugin.test.base.TestingContext
import java.io.File
import java.lang.RuntimeException
import java.nio.file.Files

open class StraightContext(private val taskFactory: (String) -> CutterConfig) : TestingContext() {

    override fun prepare() {}

    override fun compileFolder(root: File, conf: String?): File {
        val compiled = compileSources(root)
        if (conf == null) return compiled


        val pipeline = CutterProcessingUnit.makePipeline(taskFactory(conf))
        val mincer = Mincer.Builder(MincerDecentFS(compiled))
            .registerPipeline(pipeline)
            .build()
        MincerUtils.processFolder(mincer, compiled)

        return compiled
    }

    companion object {
        fun compileSources(root: File): File {
            val src = Files.createTempDirectory("src").toFile()
            src.deleteRecursively()
            root.copyRecursively(src, overwrite = true)
            val annotationsRoot = resolve("ru/justagod/cutter")
            annotationsRoot.copyRecursively(src, overwrite = true)

            val compiled = File("build/tmp")
            compiled.deleteRecursively()
            compiled.mkdirs()


            val command = listOf(
                "javac",
                "-d",
                compiled.absolutePath,
                "-source", "8",
                "-target", "8",
                "-sourcepath", "\"${src.absolutePath}\""
            ) +
                    src
                        .walkTopDown()
                        .filter { it.isFile }
                        .map { it.absoluteFile.relativeTo(src.absoluteFile).path }
            println(command.joinToString(separator = " "))
            val p = ProcessBuilder(command)
                .directory(src.absoluteFile)
                .inheritIO()
                .start()
            val exitCode = p.waitFor()
            if (exitCode != 0) throw RuntimeException("Cannot compile source")

            return compiled
        }
    }
}