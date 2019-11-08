package ru.justagod.plugin.test.gradle

import ru.justagod.mincer.Mincer
import ru.justagod.mincer.filter.WalkThroughFilter
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.util.MincerUtils
import ru.justagod.mincer.util.MincerUtils.processArchive
import ru.justagod.mincer.util.makeFirstSimple
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.CutterPipelines
import ru.justagod.plugin.test.straight.StraightCommon
import java.io.File
import java.lang.RuntimeException
import java.net.URL
import ru.justagod.plugin.data.SideName.Companion.make as makeSide

open class GradleCommon {

    private val root = File("gradle-test")

    protected fun setUpDirectory(gradleScript: String): File {
        root.deleteRecursively()
        root.mkdirs()

        val dist = root.resolve("cutter.jar")
        dist.delete()
        File("build/libs/cutter.jar").copyTo(dist)

        root.resolve("settings.gradle").writeText("include 'gradle-test'")
        root.resolve("build.gradle").writeText(
                """
                    |import ru.justagod.plugin.gradle.CutterPlugin
                    |buildscript {
                    |    repositories {
                    |        mavenCentral()
                    |    }
                    |    dependencies {
                    |        classpath files(rootProject.file('cutter.jar'))
                    |        
                    |        classpath gradleApi()
                    |        classpath localGroovy()
                    |        classpath 'org.zeroturnaround:zt-zip:1.12'
                    |        classpath group: 'org.ow2.asm', name: 'asm', version: '6.0'
                    |        classpath group: 'org.ow2.asm', name: 'asm-commons', version: '6.0'
                    |        classpath group: 'org.ow2.asm', name: 'asm-tree', version: '6.0'
                    |        classpath group: 'org.ow2.asm', name: 'asm-util', version: '6.0'
                    |        classpath "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.2.40"
                    |        classpath group: 'org.jetbrains.kotlin', name: 'kotlin-reflect', version: '1.2.40'
                    |    }
                    |}
                    |
                    |apply plugin: 'java'
                    |apply plugin: CutterPlugin
                    |
                    |jar {
                    |   archiveName = 'mod.jar'
                    |}
                    |
                    |$gradleScript
                """.trimMargin()
        )
        val srcDir = root.resolve("src").resolve("main").resolve("java")
        srcDir.mkdirs()

        return srcDir
    }

    protected fun buildJar(name: String) {
        val pb = ProcessBuilder()
        pb.inheritIO()
        pb.command("gradle", "build" + name.capitalize(), "--no-daemon", "--stacktrace")
        pb.directory(root)
        val p = pb.start()
        val code = p.waitFor()
        if (code != 0) throw RuntimeException("Bad exit code $code")
    }

    protected fun test(src: String, targetSide: String, verifier: SubMincer<Unit, Unit>) {
        val srcDist = root
                .resolve("src")
                .resolve("main")
                .resolve("java")
        srcDist.deleteRecursively()
        srcDist.mkdirs()

        val srcEmitter = resolve(src).toFile()

        val annoDist = srcDist.resolve("anno").also { it.mkdirs() }
        val generalDist = srcDist.resolve(srcEmitter.name).also { it.mkdirs() }

        srcEmitter.copyRecursively(generalDist, overwrite = true)
        resolve("/anno").toFile().copyRecursively(annoDist, overwrite = true)

        buildJar(targetSide.toLowerCase())
        verify(targetSide, verifier)
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

    protected fun verify(targetSide: String, verifier: SubMincer<Unit, Unit>) {
        val archive = root
                .resolve("build")
                .resolve("libs")
                .resolve("mod-${targetSide.toLowerCase()}.jar")
        assert(archive.exists())
        assert(archive.isFile)
        processArchive(archive) {
            Mincer.Builder(it, false)
                    .registerSubMincer(
                            Pipeline.makeFirstSimple(
                                    verifier,
                                    WalkThroughFilter,
                                    Unit
                            )
                    )
                    .build()
        }
    }


}