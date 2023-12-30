package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.Test
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.test.base.context.StraightContext
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.config.InvokeClass
import ru.justagod.processing.cutter.config.MethodDesc
import ru.justagod.processing.cutter.config.SideName
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object InvokeClassesTests {

    @Test
    fun server() {
        val context = makeSimpleContext()
        context.prepare()

        val server = context.compileResourceFolder("test9", "server")
        runServerCheck(server)
    }

    @Test
    fun client() {
        val context = makeSimpleContext()
        context.prepare()

        val client = context.compileResourceFolder("test9", "client")
        runClientCheck(client)
    }

    private fun makeSimpleContext() = StraightContext { name ->
        CutterConfig(
            annotation = ClassTypeReference("ru.justagod.cutter.GradleSideOnly"),
            validationOverrideAnnotation = null,
            primalSides = setOf(SideName.make("SERVER"), SideName.make("CLIENT")),
            targetSides = setOf(SideName.make(name)),
            invocators = listOf(
                InvokeClass(
                    ClassTypeReference("ru.justagod.cutter.invoke.InvokeServer"),
                    hashSetOf(SideName.make("SERVER")),
                    MethodDesc("run", "()V")
                ),
                InvokeClass(
                    ClassTypeReference("ru.justagod.cutter.invoke.InvokeClient"),
                    hashSetOf(SideName.make("CLIENT")),
                    MethodDesc("run", "()V")
                ),
                InvokeClass(
                    ClassTypeReference("ru.justagod.cutter.invoke.InvokeServerValue"),
                    hashSetOf(SideName.make("SERVER")),
                    MethodDesc("run", "()Ljava/lang/Object;")
                ),
                InvokeClass(
                    ClassTypeReference("ru.justagod.cutter.invoke.InvokeClientValue"),
                    hashSetOf(SideName.make("CLIENT")),
                    MethodDesc("run", "()Ljava/lang/Object;")
                )
            ),
            removeAnnotations = false
        )
    }


    private fun runServerCheck(compiled: File) {
        runAndCheck(
            compiled, """
                Hello server lambda
                Hello server anonymous
                
            """.trimIndent()
        )
    }

    private fun runClientCheck(compiled: File) {
        runAndCheck(
            compiled, """
                Hello client lambda
                Hello client anonymous
                
            """.trimIndent()
        )
    }


    private fun runAndCheck(compiled: File, expected: String) {
        val pb = ProcessBuilder("java", "-cp", compiled.absolutePath, "test9.Simple")
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