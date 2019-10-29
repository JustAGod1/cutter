package ru.justagod.plugin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskState
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.CutterConfig
import ru.justagod.plugin.data.CutterInvokeData
import ru.justagod.plugin.data.CutterTaskData

class CutterPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def tasksContainer = project.container(CutterTaskData)
        def invokesContainer = project.container(CutterInvokeData)
        def config = project.extensions.create("cutter", CutterConfig, tasksContainer, invokesContainer)

        config.builds().forEach {
            project.tasks.register("build" + it.name.capitalize(), CutterTask) {

            }
        }
    }

}
