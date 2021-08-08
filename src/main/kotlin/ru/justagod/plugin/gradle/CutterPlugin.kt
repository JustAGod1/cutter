package ru.justagod.plugin.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.file.RelativePathSpec
import org.gradle.api.specs.CompositeSpec
import org.gradle.api.specs.OrSpec
import org.gradle.api.specs.Spec
import org.gradle.api.specs.Specs
import ru.justagod.model.ClassTypeReference

class CutterPlugin : Plugin<Project> {

    init {
        instance = this
    }

    lateinit var config: CutterConfig
        private set

    @Override
    override fun apply(project: Project) {
        val tasksContainer = project.container(CutterTaskData::class.java)


        project.extensions.create()

        config = project.extensions.create("cutter", CutterConfig::class.java, tasksContainer, project)



        val taskAll = project.getTasks().create("buildAll") {
            it.group = "build"
        }
        config.builds.all { data ->
            val dataHarvester = {
                val invokeClasses = config.invokes.map { parse(it) }
                val excludeSpec = if (config.excludes.isNotEmpty()) OrSpec<RelativePath>(config.excludes) else Spec<RelativePath> { false }
                BakedCutterTaskData(
                        data.name,
                        ClassTypeReference(config.annotation ?: error("You have to define annotation name")),
                        config.validationOverriderAnnotation?.let { ClassTypeReference(it) },
                        data.removeAnnotations && config.removeAnnotations,
                        data.primalSides.toSet(), data.targetSides.toSet(),
                        invokeClasses,
                        config.markers,
                        { excludeSpec.isSatisfiedBy(RelativePath.parse(true, it)) }

                )
            }
            val task = project.getTasks().create("build" + data.name.capitalize(), CutterTask::class.java)
            task.dataHarvester = dataHarvester
            task.archiveName = { data.archiveName }
            task.group = "build"
            task.dependsOn(project.tasks.getByName("build"))
            taskAll.dependsOn(task)
        }
    }

    private fun parse(data: InvocationClassData): InvokeClass {
        val methodName = data.method?.substringBefore("(") ?: error("You have to define method signature")
        val methodDesc = data.method!!.substring(methodName.length)
        return InvokeClass(
                ClassTypeReference(data.name ?: error("You have to define invocator class name")),
                data.sides?.toSet() ?: error("You have to define invokator's target sides"),
                MethodDesc(methodName, methodDesc)
        )
    }

    companion object {
        @JvmStatic
        lateinit var instance: CutterPlugin
    }

}
