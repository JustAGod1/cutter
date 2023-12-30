package ru.justagod.plugin.test.tests.trivial

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.pipeline.MincerPipeline
import ru.justagod.mincer.util.MincerDecentFS
import ru.justagod.mincer.util.MincerUtils
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.test.base.TestingContext
import ru.justagod.plugin.test.base.context.GradleContext
import ru.justagod.plugin.test.base.context.StraightContext
import ru.justagod.plugin.test.tests.trivial.model.TrivialTestData
import ru.justagod.plugin.test.tests.trivial.model.TrivialTestBuilder
import ru.justagod.plugin.test.tests.trivial.model.TrivialValidator
import ru.justagod.processing.cutter.CutterProcessingUnit.client
import ru.justagod.processing.cutter.CutterProcessingUnit.server
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.config.SideName
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
object CutterTrivialTests {

    private val registry = arrayListOf<TrivialTestData>()

    init {
        registry += TrivialTestBuilder("Just 2 methods")
            .src("test1")
            .conf("server", "client")
            .model {
                dir("test1") {
                    klass("Simple") {
                        method("client", "()V").conf("client")
                        method("server", "()V").conf("server")
                    }
                }
            }.build()

        registry += TrivialTestBuilder("Two classes with anonymous classes")
            .src("test2")
            .conf("server", "client")
            .model {
                dir("test2") {
                    klass("Simple1").conf("server")
                    klass("Simple1$1").conf("server")
                    klass("Simple2").conf("client")
                    klass("Simple2$1").conf("client")
                }
            }.build()

        registry += TrivialTestBuilder("Two opposite packages")
            .src("test3")
            .conf("client", "server")
            .model {
                dir("test3") {
                    dir("client").conf("client")
                    dir("server").conf("server")
                }
            }.build()

        registry += TrivialTestBuilder("Methods with different descs and inheritance")
            .src("test4")
            .conf("client", "server")
            .model {
                dir("test4") {
                    klass("Class1") {
                        method("server", "()V").conf("server")
                        method("server", "(I)V")
                        method("client", "()V").conf("client")
                        method("client", "(I)V")
                    }
                    klass("Class2") {
                        method("server", "()V").conf("server")
                        method("server", "(I)V")
                        method("client", "()V").conf("client")
                        method("client", "(I)V")
                    }
                }
            }.build()

        registry += TrivialTestBuilder("Single anonymous class")
            .src("test5")
            .conf("client", "server")
            .model {
                dir("test5") {
                    klass("Simple") {
                        method("server", "()V").conf("server")
                    }
                    klass("Simple$1").conf("server")
                }
            }
            .build()

        registry += TrivialTestBuilder("Lambda's impl method")
            .src("test6")
            .conf("client", "server")
            .model {
                dir("test6") {
                    klass("Simple") {
                        method("a", "()V").conf("server")
                        method("lambda\$a$0", "()V").conf("server")
                    }
                }
            }.build()

        registry += TrivialTestBuilder("Just 2 opposite fields")
            .src("test7")
            .conf("client", "server")
            .model {
                dir("test7") {
                    klass("Simple") {
                        field("server", "I").conf("server")
                        field("client", "I").conf("client")
                    }
                }
            }.build()
    }

    @ParameterizedTest
    @ValueSource(strings = ["8.5", "8.4", "7.3", "7.0", "6.7", "6.1", "5.3"])
    fun `Generic gradle version test to be sure we still support all gradle versions`(version: String) {
        val context = GradleContext(File("gradle-test"))
        context.version = version
        context.prepare()
        context.buildScriptWithPlugin("")

        assert(run(registry[4], context))
    }

    @TestFactory
    @DisplayName("Test everything on Gradle 5.3")
    fun fullGradle53(): List<DynamicTest> {
        return fullGradleTest("5.3")
    }

    @TestFactory
    @DisplayName("Test everything on Gradle 8.5")
    fun fullGradle85(): List<DynamicTest> {
        return fullGradleTest("8.5")
    }

    private fun fullGradleTest(version: String): List<DynamicTest> {
        val context = GradleContext(File("gradle-test"))
        context.version = version
        context.prepare()
        context.buildScriptWithPlugin("")
        return registry.map {
            DynamicTest.dynamicTest(it.name) {
                assert(run(it, context))
            }
        }
    }

    @TestFactory
    fun `Straight tests to be sure that everything works`(): List<DynamicTest> {
        val context = StraightContext { name ->
            CutterConfig(
                annotation = ClassTypeReference("ru.justagod.cutter.GradleSideOnly"),
                validationOverrideAnnotation = null,
                primalSides = setOf(server, client),
                targetSides = setOf(SideName.make(name)),
                invocators = emptyList(),
                removeAnnotations = false
            )
        }
        context.prepare()
        return registry.map {
            DynamicTest.dynamicTest(it.name) {
                assert(run(it, context))
            }
        }
    }


    private fun run(testData: TrivialTestData, context: TestingContext): Boolean {
        for (config in testData.configNames) {
            val classes = context.compileResourceFolder("trivial/" + testData.src, config)
            println("Validating $config config...")
            if (!validate(testData, classes, config)) return false
        }
        return true
    }

    private fun validate(testData: TrivialTestData, compiled: File, configName: String): Boolean {
        val pipeline = MincerPipeline
            .make(
                TrivialValidator(configName, testData.model),
                true
            )
            .build()
        val mincer = Mincer.Builder(MincerDecentFS(compiled))
            .registerPipeline(
                pipeline
            )
            .build()

        MincerUtils.processFolder(mincer, compiled, threadsCount = 1)

        return pipeline.result()
    }


}