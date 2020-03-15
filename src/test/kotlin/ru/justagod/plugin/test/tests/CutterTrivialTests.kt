package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.BakedCutterTaskData
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.test.base.TestRunner
import ru.justagod.plugin.test.base.context.ForgeContext
import ru.justagod.plugin.test.base.context.GradleContext
import ru.justagod.plugin.test.base.context.StraightContext
import ru.justagod.plugin.test.base.runner.TrivialTestRunner
import ru.justagod.plugin.test.base.runner.trivial.TrivialTestData
import ru.justagod.plugin.test.base.runner.trivial.TrivialTestBuilder

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

    private fun tests(): List<TestRunner> = registry.map { TrivialTestRunner(it) }
    private val script = """
        cutter {
                annotation = "ru.justagod.cutter.GradleSideOnly"
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
            }
    """.trimIndent()

    @TestFactory
    fun gradleTasks(): List<DynamicTest> {
        val context = GradleContext(script)
        context.before()
        return tests().map {
            DynamicTest.dynamicTest(it.name) { assert(it.run(context)) }
        }
    }

    @TestFactory
    fun forge1710(): List<DynamicTest> {
        val context = ForgeContext("1.7.10", script)
        context.before()
        return tests().map {
            DynamicTest.dynamicTest(it.name) { assert(it.run(context)) }
        }
    }

    @TestFactory
    fun forge18(): List<DynamicTest> {
        val context = ForgeContext("1.8", script)
        context.before()
        return tests().map {
            DynamicTest.dynamicTest(it.name) { assert(it.run(context)) }
        }
    }

    @TestFactory
    fun forge1122(): List<DynamicTest> {
        val context = ForgeContext("1.12.2", script)
        context.before()
        return tests().map {
            DynamicTest.dynamicTest(it.name) { assert(it.run(context)) }
        }
    }

    @TestFactory
    fun gradleTasksDef(): List<DynamicTest> {
        val context = GradleContext(GradleContext.defaultGradleScript).default()
        context.before()
        return tests().map {
            DynamicTest.dynamicTest(it.name) { assert(it.run(context)) }
        }
    }

    @TestFactory
    fun forge1710Def(): List<DynamicTest> {
        val context = ForgeContext("1.7.10", GradleContext.defaultGradleScript).default()
        context.before()
        return tests().map {
            DynamicTest.dynamicTest(it.name) { assert(it.run(context)) }
        }
    }

    @TestFactory
    fun forge18Def(): List<DynamicTest> {
        val context = ForgeContext("1.8", GradleContext.defaultGradleScript).default()
        context.before()
        return tests().map {
            DynamicTest.dynamicTest(it.name) { assert(it.run(context)) }
        }
    }

    @TestFactory
    fun forge1122Def(): List<DynamicTest> {
        val context = ForgeContext("1.12.2", GradleContext.defaultGradleScript).default()
        context.before()
        return tests().map {
            DynamicTest.dynamicTest(it.name) { assert(it.run(context)) }
        }
    }

    @TestFactory
    fun straightTests(): List<DynamicTest> {
        val context = StraightContext { name ->
            BakedCutterTaskData(
                    name = name,
                    annotation = ClassTypeReference("ru.justagod.cutter.GradleSideOnly"),
                    validationOverrideAnnotation = null,
                    removeAnnotations = false,
                    primalSides = setOf(SideName.make("server"), SideName.make("client")),
                    targetSides = setOf(SideName.make(name)),
                    invocators = emptyList(),
                    markers = emptyList()
            )
        }
        context.before()
        return tests().map {
            DynamicTest.dynamicTest(it.name) { assert(it.run(context)) }
        }
    }

}