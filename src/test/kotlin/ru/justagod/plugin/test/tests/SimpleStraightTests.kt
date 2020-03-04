package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.test.base.context.StraightContext

object SimpleStraightTests : StraightContext() {
    override fun makeTask(name: String): CutterTaskData {
        val data = CutterTaskData(name)
        data.invokeClasses = emptyList()
        data.primalSides = listOf(SideName.make("server"), SideName.make("client"))
        data.targetSides = listOf(SideName.make(name))
        return data
    }

    @TestFactory
    fun makeTasks(): List<DynamicTest> {
        val tests = CutterTrivialTests.tests().map {
            DynamicTest.dynamicTest(it.name) { assert(it.run(this)) }
        }

        return tests
    }
}