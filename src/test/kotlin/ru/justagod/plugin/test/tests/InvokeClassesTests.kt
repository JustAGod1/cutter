package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.Test
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.BakedCutterTaskData
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.model.InvokeClass
import ru.justagod.plugin.processing.model.MethodDesc
import ru.justagod.plugin.test.base.TestRunner
import ru.justagod.plugin.test.base.TestingContext
import ru.justagod.plugin.test.base.context.ForgeContext
import ru.justagod.plugin.test.base.context.GradleContext
import ru.justagod.plugin.test.base.context.StraightContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object InvokeClassesTests : TestRunner {

    @Test
    fun straight() {
        val context = StraightContext { name ->
            BakedCutterTaskData(
                    name = name,
                    annotation = ClassTypeReference("ru.justagod.cutter.GradleSideOnly"),
                    validationOverrideAnnotation = null,
                    removeAnnotations = false,
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
                            )
                    ),
                    markers = emptyList(),
                    cuttingMarkers = emptyList()

            )
        }
        context.before()
        run(context)
    }

    @Test
    fun gradle() {
        val context = GradleContext(gradleScript)
        context.before()
        run(context)
    }

    @Test
    fun forge1710() {
        val context = ForgeContext("1.7.10", gradleScript)
        context.before()
        run(context)
    }

    @Test
    fun forge18() {
        val context = ForgeContext("1.8", gradleScript)
        context.before()
        run(context)
    }

    @Test
    fun forge1122() {
        val context = ForgeContext("1.12.2", gradleScript)
        context.before()
        run(context)
    }

    @Test
    fun gradleDef() {
        val context = GradleContext(GradleContext.defaultGradleScript).default()
        context.before()
        run(context)
    }

    @Test
    fun forge1710Def() {
        val context = ForgeContext("1.7.10", GradleContext.defaultGradleScript).default()
        context.before()
        run(context)
    }

    @Test
    fun forge18Def() {
        val context = ForgeContext("1.8", GradleContext.defaultGradleScript).default()
        context.before()
        run(context)
    }

    @Test
    fun forge1122Def() {
        val context = ForgeContext("1.12.2", GradleContext.defaultGradleScript).default()
        context.before()
        run(context)
    }

    private val gradleScript = """
            cutter {
                annotation = "ru.justagod.cutter.GradleSideOnly"
                def serverSide = side('server')
                def clientSide = side('client')
                invocation {
                    name = 'ru.justagod.cutter.invoke.InvokeServer'
                    sides = [serverSide]
                    method = 'run()V'
                }
                invocation {
                    name = 'ru.justagod.cutter.invoke.InvokeClient'
                    sides = [clientSide]
                    method = 'run()V'
                }
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
            }
        """.trimIndent()

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