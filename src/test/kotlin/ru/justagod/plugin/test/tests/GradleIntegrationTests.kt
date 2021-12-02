package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.Test
import ru.justagod.plugin.test.base.context.GradleContext

import org.assertj.core.api.Assertions.*
import java.io.File

class GradleIntegrationTests {


    @Test
    fun `Tasks depending on jar actually mean smth`() {
        val fileName = "build/classes/java/main/kek"
        val expected = "kek"

        val gradle = GradleContext()
        gradle.prepare()

        gradle.root.resolve(fileName).parentFile.mkdirs()

        val script = """
            task kek {
                doLast {
                    project.file("$fileName").getParentFile().mkdirs()
                    project.file("$fileName").write("$expected")
                    jar.from(project.file("$fileName"))
                }
            }
            
            jar.dependsOn(kek)
        """.trimIndent()

        gradle.buildScriptWithPlugin(script)
        gradle.run("buildServer")

        val unpacked = gradle.unpackArtifact("modid-1.0-server.jar")

        assertThat(unpacked.resolve(expected)).isFile
        assertThat(unpacked.resolve(expected)).hasContent(expected)
    }

    @Test
    fun `Tasks depending on build actually mean smth`() {
        val fileName = "build/classes/java/main/kek"
        val expected = "kek"

        val gradle = GradleContext()
        gradle.prepare()

        gradle.root.resolve(fileName).parentFile.mkdirs()

        val script = """
            task kek {
                doFirst {
                    project.file("$fileName").getParentFile().mkdirs()
                    project.file("$fileName").write("$expected")
                    jar.from(project.file("$fileName"))
                }
            }
            
            jar.dependsOn(kek)
        """.trimIndent()

        gradle.buildScriptWithPlugin(script)
        gradle.run("buildServer")

        val unpacked = gradle.unpackArtifact("modid-1.0-server.jar")

        assertThat(unpacked.resolve(expected)).isFile
        assertThat(unpacked.resolve(expected)).hasContent(expected)
    }

    @Test
    fun `User classes should be presented`() {
        val className = "A"

        val gradle = GradleContext()
        gradle.prepare()

        gradle.addSource(className, "class A {}")

        gradle.buildScriptWithPlugin()
        gradle.run("buildServer")

        val unpacked = gradle.unpackArtifact("modid-1.0-server.jar")

        assertThat(unpacked.resolve("$className.class")).isFile
    }
}