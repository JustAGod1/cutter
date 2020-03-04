package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import ru.justagod.plugin.test.base.context.GradleContext

object SimpleGradleTests : GradleContext() {
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
            }
    """.trimIndent()

    init {
        before()
    }

    @TestFactory
    fun makeTasks(): List<DynamicTest> {
        val tests = CutterTrivialTests.tests().map {
            DynamicTest.dynamicTest(it.name) { assert(it.run(this)) }
        }

        return tests
    }
}