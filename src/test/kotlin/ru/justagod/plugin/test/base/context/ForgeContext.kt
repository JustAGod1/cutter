package ru.justagod.plugin.test.base.context

import java.io.File
import java.lang.RuntimeException

class ForgeContext(private val forgeVersion: String, gradleScript: String) : GradleContext(gradleScript) {

    override fun makeBuildGradle() {
        File("forge").resolve(forgeVersion).copyRecursively(root, false) { _, err ->
            throw RuntimeException("Cannot copy", err)
        }
        val exitCode = ProcessBuilder("chmod", "+rwx", root.resolve("gradlew").absolutePath).inheritIO().start().waitFor()
        if (exitCode != 0) error(exitCode.toString())
        root.resolve("build.gradle").appendText(gradleScript)
    }

    override fun prepare() {
        runGradleCommand("setupDecompWorkspace")
    }

    override fun runGradleCommand(vararg args: String) {
        val pb = ProcessBuilder()
        pb.inheritIO()
        pb.command(listOf("./gradlew") + args + "--no-daemon" + "--stacktrace")
        pb.directory(root)
        val p = pb.start()
        val code = p.waitFor()
        if (code != 0) throw RuntimeException("Bad exit code $code")
    }
}