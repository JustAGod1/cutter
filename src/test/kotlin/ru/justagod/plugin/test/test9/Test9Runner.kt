package ru.justagod.plugin.test.test9

import ru.justagod.mincer.Mincer
import ru.justagod.mincer.util.MincerDecentFS
import ru.justagod.mincer.util.MincerUtils
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.CutterPipelines
import ru.justagod.plugin.processing.model.InvokeClass
import ru.justagod.plugin.processing.model.MethodDesc
import ru.justagod.plugin.test.straight.StraightCommon
import java.io.*
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

object Test9Runner : StraightCommon() {

    fun run() {
        run {
            val compiled = compile(Test9Runner.javaClass.getResource("/test9")!!)
            runAndCheck(compiled, """
                Hello client lambda
                Hello client anonymous
                Hello server lambda
                Hello server anonymous
                
            """.trimIndent())
        }

        run {
            val compiled = compile(Test9Runner.javaClass.getResource("/test9")!!)
            process(compiled, "SERVER")
            runAndCheck(compiled, """
                Hello server lambda
                Hello server anonymous
                
            """.trimIndent())
        }

        run {
            val compiled = compile(Test9Runner.javaClass.getResource("/test9")!!)
            process(compiled, "CLIENT")
            runAndCheck(compiled, """
                Hello client lambda
                Hello client anonymous
                
            """.trimIndent())
        }
    }

    private fun process(compiled: File, side: String) {
        val fs = MincerDecentFS(compiled)
        val taskData = CutterTaskData(side)
        taskData.primalSides = listOf(
                SideName.make("SERVER"),
                SideName.make("CLIENT")
        )
        taskData.targetSides = listOf(
                SideName.make(side)
        )
        taskData.invokeClasses = listOf(
                InvokeClass(
                        ClassTypeReference("test9.ServerInvoke"),
                        hashSetOf(SideName.make("SERVER")),
                        MethodDesc("run", "()V")
                ),
                InvokeClass(
                        ClassTypeReference("test9.ClientInvoke"),
                        hashSetOf(SideName.make("CLIENT")),
                        MethodDesc("run", "()V")
                )
        )
        val mincer = Mincer.Builder(fs, false)
                .registerSubMincer(
                        CutterPipelines.makePipeline("anno.SideOnly", taskData)
                )
                .build()

        MincerUtils.processFolder(mincer, compiled)
    }

    private fun runAndCheck(compiled: File, expected: String) {
        val javaPath = File(System.getenv("JAVA_HOME")).resolve("bin").resolve("java.exe")
        val pb = ProcessBuilder("\"${javaPath.absolutePath}\"", "-cp", "\"${compiled.absolutePath}\"", "test9.Simple")
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