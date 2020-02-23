package ru.justagod.plugin.gradle

import kotlin.text.StringsKt
import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.CutterConfig
import ru.justagod.plugin.data.CutterTaskData
import ru.justagod.plugin.data.InvocationClassData
import ru.justagod.plugin.processing.model.InvokeClass
import ru.justagod.plugin.processing.model.MethodDesc

class CutterPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        println("Initializing cutter plugin")
        def tasksContainer = project.container(CutterTaskData)
        def config = project.extensions.create("cutter", CutterConfig, tasksContainer)

        def taskAll = project.task("buildAll") {
            group = 'build'
        }
        config.builds.all { data ->
            def task = project.tasks.register("build" + data.name.capitalize(), CutterTask) {
                it.group = 'build'
                def invokeClasses = config.invokes.collect { parse(it) }
                it.dependsOn(project.tasks.jar)
                it.data = data
                it.data.invokeClasses = invokeClasses
                it.config = config
            }
            taskAll.dependsOn(task)
        }
    }

    private static InvokeClass parse(InvocationClassData data) {
        def methodName = StringsKt.substringBefore(data.method, "(", data.method)
        def methodDesc = data.method.substring(methodName.size())
        return new InvokeClass(
                new ClassTypeReference(data.name),
                data.sides.toSet(),
                new MethodDesc(methodName, methodDesc)
        )
    }

}
