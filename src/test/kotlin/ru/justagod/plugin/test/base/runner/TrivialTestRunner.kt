package ru.justagod.plugin.test.base.runner

import ru.justagod.mincer.MincerBuilder
import ru.justagod.mincer.filter.WalkThroughFilter
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.util.MincerDecentFS
import ru.justagod.mincer.util.MincerUtils
import ru.justagod.mincer.util.makeFirstSimple
import ru.justagod.plugin.test.base.TestRunner
import ru.justagod.plugin.test.base.TestingContext
import ru.justagod.plugin.test.base.runner.trivial.TrivialTestData
import ru.justagod.plugin.test.base.runner.trivial.TrivialValidator
import java.io.File

class TrivialTestRunner(private val testData: TrivialTestData) : TestRunner {
    override val name: String
        get() = testData.name

    override fun run(context: TestingContext): Boolean {
        for (config in testData.configNames) {
            val classes = context.compileResourceFolder("trivial/" + testData.src, config)
            println("Validating $config config...")
            if (!validate(classes, config)) return false
        }
        return true
    }

    private fun validate(compiled: File, configName: String): Boolean {
        val pipeline = Pipeline
                .makeFirstSimple(
                        TrivialValidator(configName, testData.model),
                        WalkThroughFilter, null
                )
        val mincer = MincerBuilder(MincerDecentFS(compiled), false)
                .registerSubMincer(
                        pipeline
                )
                .build()

        MincerUtils.processFolder(mincer, compiled)

        return pipeline.value!!
    }


}

