package ru.justagod.plugin.test.straight

import org.gradle.api.internal.tasks.compile.DefaultJavaCompilerFactory
import org.gradle.api.internal.tasks.compile.JavaHomeBasedJavaCompilerFactory
import ru.justagod.mincer.control.MincerControlPane
import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.util.MincerDecentFS
import ru.justagod.mincer.util.MincerFactory
import ru.justagod.mincer.util.MincerUtils
import java.io.File
import java.lang.RuntimeException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import javax.tools.JavaCompiler

open class StraightCommon {

    protected fun compile(root: URL): File {
        val src = Files.createTempDirectory("src").toFile()
        src.deleteRecursively()
        root.toFile().copyRecursively(src, overwrite = true)
        val annotationsRoot = resolve("anno").toFile()
        annotationsRoot.copyRecursively(src, overwrite = true)

        val compiled = Files.createTempDirectory("out").toFile()

        val command = listOf(
                "javac",
                "-d",
                "\"${compiled.absolutePath}\"",
                "-source", "8",
                "-target", "8",
                "-sourcepath", "\"${src.absolutePath}\"") +
                src
                        .walkTopDown()
                        .filter { it.isFile }
                        .map { it.absoluteFile.relativeTo(src.absoluteFile).path }
        val p = ProcessBuilder(command)
                .directory(src.absoluteFile)
                .inheritIO()
                .start()
        val exitCode = p.waitFor()
        if (exitCode != 0) throw RuntimeException("Cannot compile source")
        return compiled
    }

    protected fun compileAndProcess(src: URL, factory: MincerFactory): OptionalVerification {
        val compiled = compile(src)
        val fs = MincerDecentFS(compiled)
        val mincer = factory(fs)
        MincerUtils.processFolder(mincer, compiled)

        return OptionalVerification(compiled, fs)
    }

    protected fun resolve(name: String): URL {
        return if (!name.startsWith("/")) {
            StraightCommon::class.java.getResource("/$name")!!
        } else {
            StraightCommon::class.java.getResource(name)!!
        }
    }

    private fun URL.toFile(): File {
        return File(this.path)
    }

    protected class OptionalVerification(private val root: File, private val fs: MincerFS) {

        fun validate(factory: MincerFactory) {
            val fs = MincerDecentFS(root)
            val mincer = factory(fs)
            MincerUtils.processFolder(mincer, root)
        }
    }
}