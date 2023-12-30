package ru.justagod.plugin.test.base.context

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.zeroturnaround.zip.ZipUtil
import ru.justagod.plugin.test.base.TestingContext
import java.io.File
import java.nio.file.Files

class GradleContext(val root: File = File("gradle-test")) : TestingContext() {

    var version = "5.3"

    companion object {
        const val scriptName = "build.gradle"
        const val sourceDir = "src/main/java"
        const val pluginJarName = "cutter.jar"
        const val propertiesName = "gradle.properties"
        const val settingsName = "settings.gradle"
    }

    fun buildScript(script: String) {
        root.resolve(scriptName).writeText(script)
    }

    fun buildScriptWithPlugin() {
        buildScriptWithPlugin("")
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
                    |   archiveFileName.set('mod.jar')
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
        root.resolve(sourceDir).resolve("$name.java").writeText(code)

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

    override fun prepare() {
        root.deleteRecursively()
        root.mkdirs()
        copyPluginJar()
        makeSourceDirFolder()
        makeGradleSettings(root.name)
        putGradleJVMArgs()
    }

    private fun putGradleJVMArgs() {
        if (System.getenv("debug") != "true") return
        val fileLeakFile = File("file-leak-detector-1.13-jar-with-dependencies.jar")
        val fileLeakAbs = fileLeakFile.absolutePath.replace("\\", "/")

        val sb = StringBuilder()

        sb.append("\\\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5006\\\" ")
        sb.append("-javaagent:$fileLeakAbs=http=19999")
        val f = root.resolve(propertiesName)
        f.writeText("org.gradle.jvmargs=$sb")
    }

    override fun compileFolder(root: File, conf: String?): File {
        insertSources(root)
        val artifact = if (conf != null) {
            run("clean", "build" + conf.capitalize())
            "modid-1.0-${conf.toLowerCase()}.jar"
        } else {
            run("clean", "build")
            "modid-1.0.jar"
        }

        return unpackArtifact(artifact)
    }

    fun unpackArtifact(name: String): File {
        val unpackTarget = Files.createTempDirectory("out").toFile()
        val jarFile = root.resolve("build").resolve("libs").resolve(name)

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