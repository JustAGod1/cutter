package ru.justagod.plugin.test.tests

import ru.justagod.plugin.test.base.TestRunner
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
                        klass("Simple1$0").conf("server")
                        klass("Simple2").conf("client")
                        klass("Simple2$0").conf("client")
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
                        klass("Simple$0").conf("server")
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

    fun tests(): List<TestRunner> = registry.map { TrivialTestRunner(it) }

}