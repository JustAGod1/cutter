package ru.justagod.plugin.test.base.runner.trivial

import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.InheritanceHelper

class TrivialValidator(private val configName: String, private val imageModel: ImageModel)
    : SubMincer<Unit, Boolean> {

    private val notFoundEntries = imageModel.getMandatoryEntries(configName).toHashSet()
    private val unexpectedEntries = hashSetOf<ImageModel.ProjectEntry>()

    override fun process(context: WorkerContext<Unit, Boolean>): MincerResultType {
        val path = context.name.name.split(".").dropLast(1)
        var currentDir = imageModel.root
        for (dir in path) {
            currentDir = currentDir.directories[dir] ?: return MincerResultType.SKIPPED
            if (!currentDir.doesExistInConf(configName)) {
                unexpectedEntries += currentDir
                return MincerResultType.SKIPPED
            }
            notFoundEntries -= currentDir
        }
        val klass = currentDir.classes[context.name.name.split('.').last()]
                ?: return MincerResultType.SKIPPED
        if (!klass.doesExistInConf(configName)) {
            unexpectedEntries += klass
            return MincerResultType.SKIPPED
        }
        notFoundEntries -= klass

        context.info!!.node.methods?.forEach { mn ->
            val m = klass.methods[mn.name] ?: return@forEach
            val match = m.find { it.desc == mn.desc }
            if (match != null) {
                if (!match.doesExistInConf(configName)) {
                    unexpectedEntries += match
                } else {
                    notFoundEntries -= match
                }
            }
        }
        context.info!!.node.fields?.forEach { fn ->
            val f = klass.fields[fn.name] ?: return@forEach
            if (f.desc == fn.desc) {
                if (!f.doesExistInConf(configName)) {
                    unexpectedEntries += f
                } else {
                    notFoundEntries -= f
                }
            }
        }
        return MincerResultType.SKIPPED
    }

    override fun endProcessing(input: Unit, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, Boolean>) {
        if (notFoundEntries.isNotEmpty()) {
            System.err.println("Not found entries:")
            notFoundEntries.forEachIndexed { i, v ->
                System.err.println("   ${i + 1}) $v")
            }
        }
        if (unexpectedEntries.isNotEmpty()) {
            System.err.println("Unexpected entries:")
            unexpectedEntries.forEachIndexed { i, v ->
                System.err.println("   ${i + 1}) $v")
            }
        }
        pipeline.value = notFoundEntries.isEmpty() && unexpectedEntries.isEmpty()
    }

}