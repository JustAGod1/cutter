package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.Test
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.BakedCutterTaskData
import ru.justagod.plugin.data.DynSideMarkerBuilder
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.test.base.TestRunner
import ru.justagod.plugin.test.base.TestingContext
import ru.justagod.plugin.test.base.context.ForgeContext
import ru.justagod.plugin.test.base.context.GradleContext
import ru.justagod.plugin.test.base.context.StraightContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object EuristicTests : TestRunner {

    @Test
    fun straight() {
        val server = SideName.make("server")
        val client = SideName.make("client")
        val context = StraightContext { name ->
            BakedCutterTaskData(
                    name = name,
                    annotation = ClassTypeReference("ru.justagod.cutter.GradleSideOnly"),
                    validationOverrideAnnotation = null,
                    removeAnnotations = false,
                    primalSides = setOf(SideName.make("SERVER"), SideName.make("CLIENT")),
                    targetSides = setOf(SideName.make(name)),
                    invocators = emptyList(),
                    markers = listOf(
                            DynSideMarkerBuilder().method().owner("test10.SideUtil").name("isServer").desc("()Z").sides(setOf(server)).build(),
                            DynSideMarkerBuilder().method().owner("test10.SideUtil").name("isClient").desc("()Z").sides(setOf(client)).build(),
                            DynSideMarkerBuilder().field().owner("test10.SideUtil").name("isServer").sides(setOf(server)).build(),
                            DynSideMarkerBuilder().field().owner("test10.SideUtil").name("isClient").sides(setOf(client)).build()
                    )
            )
        }
        context.before()
        run(context)
    }

    override val name: String = " classes test"

    override fun run(context: TestingContext): Boolean {
        val server = context.compileResourceFolder("test10", "server")
        runServerCheck(server)

        val client = context.compileResourceFolder("test10", "client")
        runClientCheck(client)

        return true
    }


    private fun runServerCheck(compiled: File) {
        runAndCheck(compiled, """
                Server code
                Server code
                Server code
                Both
                
            """.trimIndent())
    }

    private fun runClientCheck(compiled: File) {
        runAndCheck(compiled, """
                Client code
                Client code
                Client code
                Both
                
            """.trimIndent())
    }


    private fun runAndCheck(compiled: File, expected: String) {
        val pb = ProcessBuilder("java", "-cp", compiled.absolutePath, "test10.Simple")
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
        pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        val process = pb.start()

        val buffer = StringBuilder()
        val input = BufferedReader(InputStreamReader(process.inputStream))
        while (input.ready() || process.isAlive) {
            val line = input.readLine() ?: continue
            println(line)
            buffer.append(line).append("\n")
        }

        assert(process.waitFor(10, TimeUnit.SECONDS)) { "Process is still running" }
        val exitCode = process.exitValue()
        assert(exitCode == 0) { "Process exited with code $exitCode" }
        assert(buffer.toString() == expected) { "Expected: $expected\nActual: $buffer\n" }
    }

}