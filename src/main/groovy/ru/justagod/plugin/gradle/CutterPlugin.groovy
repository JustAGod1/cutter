package ru.justagod.plugin.gradle

import groovy.transform.CompileStatic
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
        def tasksContainer = project.container(CutterTaskData)
        def config = project.extensions.create("cutter", CutterConfig, tasksContainer)

        def taskAll = project.getTasks().create("buildAll") {
            group = 'build'
        }
        if (taskAll == null) throw new RuntimeException("de")
        config.builds.all { data ->
            def task = project.getTasks().create("build" + data.name.capitalize(), CutterTask.class as Class<CutterTask>) as CutterTask;
            if (task == null) throw new RuntimeException("dede")
            task.group = 'build'
            def invokeClasses = config.invokes.collect { parse(it) }
            task.dependsOn(project.tasks.jar)
            data.invokeClasses = invokeClasses
            task.data = data
            task.config = config
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
