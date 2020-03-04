package ru.justagod.plugin.test.base.context

import org.zeroturnaround.zip.ZipUtil
import ru.justagod.plugin.test.base.TestingContext
import java.io.File
import java.lang.RuntimeException
import java.nio.file.Files

abstract class GradleContext : TestingContext() {
    private val root = File("./gradle-test")

    protected abstract fun gradleScript(): String

    override fun before() {
        root.deleteRecursively()
        root.mkdirs()

        val dist = root.resolve("cutter.jar")
        dist.delete()
        File("build/libs/cutter.jar").copyTo(dist)

        root.resolve("settings.gradle").writeText("include 'gradle-test'")
        root.resolve("./build.gradle").writeText(
                """
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
                    |apply plugin: 'cutter'
                    |
                    |jar {
                    |   archiveName = 'mod.jar'
                    |}
                    |
                    |${gradleScript()}
                """.trimMargin()
        )
        if (System.getProperty("debug") == "true") {
            root.resolve("gradle.properties").writeText("org.gradle.jvmargs=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
        }
        val srcDir = root.resolve("src").resolve("main").resolve("java")
        srcDir.mkdirs()

    }

    override fun compileFolder(root: File, name: String): File {
        insertSourcesAndClean(root)
        runGradleCommand("build" + name.capitalize())
        val jarFile = this.root.resolve("build").resolve("libs").resolve("mod-${name.toLowerCase()}.jar")

        val unpackTarget = Files.createTempDirectory("out").toFile()
        ZipUtil.unpack(jarFile, unpackTarget)

        return unpackTarget
    }


    private fun runGradleCommand(vararg args: String) {
        val pb = ProcessBuilder()
        pb.inheritIO()
        pb.command(listOf("gradle") + args + "--no-daemon" + "--stacktrace")
        pb.directory(root)
        val p = pb.start()
        val code = p.waitFor()
        if (code != 0) throw RuntimeException("Bad exit code $code")
    }

    private fun insertSources(src: File) {
        val srcDist = root
                .resolve("src")
                .resolve("main")
                .resolve("java")
        srcDist.deleteRecursively()
        srcDist.mkdirs()

        val annoDist = srcDist.resolve("anno").also { it.mkdirs() }
        val generalDist = srcDist.resolve(src.name).also { it.mkdirs() }

        src.copyRecursively(generalDist, overwrite = true)
        resolve("/anno").copyRecursively(annoDist, overwrite = true)

    }

    private fun clean() {
        runGradleCommand("clean")
    }

    private fun insertSourcesAndClean(src: File) {
        insertSources(src)
        clean()
    }
}