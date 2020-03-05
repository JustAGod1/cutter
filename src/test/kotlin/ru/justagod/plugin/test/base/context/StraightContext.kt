package ru.justagod.plugin.test.base.context

import ru.justagod.mincer.MincerBuilder
import ru.justagod.mincer.util.MincerDecentFS
import ru.justagod.mincer.util.MincerUtils
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.CutterPipelines
import ru.justagod.plugin.test.base.TestingContext
import java.io.File
import java.lang.RuntimeException
import java.nio.file.Files

class StraightContext(private val taskFactory: (String) -> CutterTaskData) : TestingContext() {

    override fun before() {}

    override fun compileFolder(root: File, name: String): File {
        val src = Files.createTempDirectory("src").toFile()
        src.deleteRecursively()
        root.copyRecursively(src, overwrite = true)
        val annotationsRoot = resolve("anno")
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
                "-sourcepath", "\"${src.absolutePath}\"") +
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


        val pipeline = CutterPipelines.makePipeline(
                "anno.SideOnly",
                taskFactory(name)
                )
        val mincer = MincerBuilder(MincerDecentFS(compiled), false)
                .registerSubMincer(pipeline)
                .build()
        MincerUtils.processFolder(mincer, compiled)

        return compiled
    }
}