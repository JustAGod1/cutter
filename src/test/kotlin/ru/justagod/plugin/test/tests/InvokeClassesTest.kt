package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.model.InvokeClass
import ru.justagod.plugin.processing.model.MethodDesc
import ru.justagod.plugin.test.base.TestRunner
import ru.justagod.plugin.test.base.TestingContext
import ru.justagod.plugin.test.base.context.GradleContext
import ru.justagod.plugin.test.base.context.StraightContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

object InvokeClassesTest : TestRunner {

    @Test
    fun straight() {
        InvocationStraightContext.before()
        run(InvocationStraightContext)
    }

    @Test
    fun gradle() {
        InvocationGradleContext.before()
        run(InvocationGradleContext)
    }

    private object InvocationGradleContext : GradleContext() {
        override fun gradleScript(): String = """
            cutter {
                annotation = "anno.SideOnly"
                def serverSide = side('SERVER')
                def clientSide = side('CLIENT')
                builds {
                    client {
                        targetSides = [clientSide]
                        primalSides = [clientSide, serverSide]
                    }
                    server {
                        targetSides = [serverSide]
                        primalSides = [clientSide, serverSide]
                    }
                }
                invocation {
                    name = 'test9.ServerInvoke'
                    sides = [serverSide]
                    method = 'run()V'
                }
                invocation {
                    name = 'test9.ClientInvoke'
                    sides = [clientSide]
                    method = 'run()V'
                }
            }
        """.trimIndent()
    }

    private object InvocationStraightContext : StraightContext() {
        override fun makeTask(name: String): CutterTaskData {
            val taskData = CutterTaskData(name)
            taskData.primalSides = listOf(
                    SideName.make("SERVER"),
                    SideName.make("CLIENT")
            )
            taskData.targetSides = listOf(
                    SideName.make(name)
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
            return taskData
        }
    }

    override val name: String = "Invocation classes test"

    override fun run(context: TestingContext): Boolean {
        val server = context.compileResourceFolder("test9", "server")
        runServerCheck(server)

        val client = context.compileResourceFolder("test9", "client")
        runClientCheck(client)

        return true
    }


    private fun runServerCheck(compiled: File) {
        runAndCheck(compiled, """
                Hello server lambda
                Hello server anonymous
                
            """.trimIndent())
    }

    private fun runClientCheck(compiled: File) {
        runAndCheck(compiled, """
                Hello client lambda
                Hello client anonymous
                
            """.trimIndent())
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