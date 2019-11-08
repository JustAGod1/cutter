package ru.justagod.plugin.gradle


import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.justagod.plugin.data.CutterConfig
import ru.justagod.plugin.data.CutterInvokeData
import ru.justagod.plugin.data.CutterTaskData

class CutterPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        println("Initializing cutter plugin")
        def tasksContainer = project.container(CutterTaskData)
        def invokesContainer = project.container(CutterInvokeData)
        def config = project.extensions.create("cutter", CutterConfig, tasksContainer, invokesContainer)

        config.builds.all { data ->
            project.tasks.register("build" + data.name.capitalize(), CutterTask) {
                it.dependsOn(project.tasks.jar)
                it.data = data
                it.config = config
            }
        }
    }

}
