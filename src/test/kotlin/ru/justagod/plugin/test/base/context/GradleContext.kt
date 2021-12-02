package ru.justagod.plugin.test.base.context

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.zeroturnaround.zip.ZipUtil
import ru.justagod.plugin.test.base.TestingContext
import java.io.File
import java.nio.file.Files

class GradleContext(private val root: File = File("gradle-test")) : TestingContext() {

    var version = "5.0"

    companion object {
        const val scriptName = "build.gradle"
        const val sourceDir = "src/main/java"
        const val pluginJarName = "cutter.jar"
        const val settingsName = "settings.gradle"
    }

    fun buildScript(script: String) {
        root.resolve(scriptName).writeText(script)
    }

    fun buildScriptWithPlugin(script: String) {
        buildScriptWithPlugin("modid", "1.0", script)
    }
    fun buildScriptWithPlugin(archiveBaseName: String, version: String, script: String) {
        val enhancedScript = """
                    |buildscript {
                    |    repositories {
                    |        mavenCentral()
                    |    }
                    |    dependencies {
                    |        classpath files(rootProject.file('$pluginJarName'))
                    |        
                    |        classpath gradleApi()
                    |        classpath localGroovy()
                    |        classpath group: 'org.ow2.asm', name: 'asm-tree', version: '8.0.1'
                    |        classpath 'org.zeroturnaround:zt-zip:1.12'
                    |        classpath group: 'org.ow2.asm', name: 'asm', version: '6.0'
                    |        classpath group: 'org.ow2.asm', name: 'asm-commons', version: '6.0'
                    |        classpath group: 'org.ow2.asm', name: 'asm-tree', version: '6.0'
                    |        classpath group: 'org.ow2.asm', name: 'asm-util', version: '6.0'
                    |        classpath "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.2.70"
                    |        classpath group: 'org.jetbrains.kotlin', name: 'kotlin-reflect', version: '1.2.70'
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
                    |$script
                    |version = "$version"
                    |archivesBaseName = "$archiveBaseName"
                """.trimMargin()

        buildScript(enhancedScript)
    }

    fun copyPluginJar() {
        val dist = root.resolve(pluginJarName)
        dist.delete()
        File("build/libs/cutter.jar").copyTo(dist)
    }

    fun makeGradleSettings(name: String) {
        val dist = root.resolve(settingsName)

        dist.writeText("rootProject.name = '$name'")
    }

    private fun makeSourceDirFolder() {
        root.resolve(sourceDir).mkdirs()
    }

    fun addSource(name: String, code: String) {
        makeSourceDirFolder()
        val dist = root.resolve(sourceDir).resolve(name)

    }

    fun run(vararg task: String): BuildResult {
        return GradleRunner.create()
            .withGradleVersion(version)
            .withArguments((task.toList() + "--stacktrace"))
            .withProjectDir(root)
            .forwardOutput()
            .build()
    }

    fun runAndFail(vararg task: String): BuildResult {
        return GradleRunner.create()
            .withGradleVersion(version)
            .withArguments((task.toList() + "--stacktrace"))
            .withProjectDir(root)
            .forwardOutput()
            .buildAndFail()
    }

    override fun before() {
        copyPluginJar()
        makeSourceDirFolder()
        makeGradleSettings(root.name)
    }

    override fun compileFolder(root: File, conf: String?): File {
        insertSources(root)
        val jarFile = if (conf != null) {
            run("clean", "build" + conf.capitalize())
            this.root.resolve("build").resolve("libs").resolve("modid-1.0-${conf.toLowerCase()}.jar")
        } else {
            run("clean", "build")
            this.root.resolve("build").resolve("libs").resolve("mod.jar")
        }
        val unpackTarget = Files.createTempDirectory("out").toFile()
        ZipUtil.unpack(jarFile, unpackTarget)

        return unpackTarget
    }

    private fun insertSources(src: File) {
        val srcDist = root.resolve(sourceDir)
        srcDist.deleteRecursively()
        srcDist.mkdirs()

        val generalDist = srcDist.resolve(src.name).also { it.mkdirs() }
        src.copyRecursively(generalDist, overwrite = true)
    }

}