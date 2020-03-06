package ru.justagod.plugin.test.base.context

import org.zeroturnaround.zip.ZipUtil
import ru.justagod.plugin.test.base.TestingContext
import java.io.File
import java.lang.RuntimeException
import java.nio.file.Files

open class GradleContext(protected val gradleScript: String) : TestingContext() {
    protected val root = File("./gradle-test")
    private var dontCopyAnno = false

    fun default(): GradleContext {
        dontCopyAnno = true
        return this
    }

    override fun before() {
        root.deleteRecursively()
        root.mkdirs()

        val dist = root.resolve("cutter.jar")
        dist.delete()
        File("build/libs/cutter.jar").copyTo(dist)

        root.resolve("settings.gradle").writeText("include 'gradle-test'")
        makeBuildGradle()
        if (System.getProperty("debug") == "true") {
            root.resolve("gradle.properties").writeText("org.gradle.jvmargs=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005\norg.gradle.jvmargs=-Dprint-sides=true")
        } else {
            root.resolve("gradle.properties").writeText("org.gradle.jvmargs=-Dprint-sides=true")
        }
        val srcDir = root.resolve("src").resolve("main").resolve("java")
        srcDir.mkdirs()
        prepare()
    }

    protected open fun prepare() {}

    protected open fun makeBuildGradle() {
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
                    |$gradleScript
                """.trimMargin()
        )
    }

    override fun compileFolder(root: File, conf: String?): File {
        insertSources(root)
        val jarFile = if (conf != null) {
            runGradleCommand("clean", "build" + conf.capitalize())
            this.root.resolve("build").resolve("libs").resolve("mod-${conf.toLowerCase()}.jar")
        } else {
            runGradleCommand("clean", "build")
            this.root.resolve("build").resolve("libs").resolve("mod.jar")
        }
        val unpackTarget = Files.createTempDirectory("out").toFile()
        ZipUtil.unpack(jarFile, unpackTarget)

        return unpackTarget
    }


    protected open fun runGradleCommand(vararg args: String) {
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

        val generalDist = srcDist.resolve(src.name).also { it.mkdirs() }
        src.copyRecursively(generalDist, overwrite = true)
        if (!dontCopyAnno) {
            val annoDist = srcDist
                    .resolve("ru").resolve("justagod").resolve("cutter")
                    .also { it.mkdirs() }
            resolve("/ru/justagod/cutter").copyRecursively(annoDist, overwrite = true)
        }

    }


    companion object {
        const val defaultGradleScript = "cutter.initializeDefault()"
    }
}