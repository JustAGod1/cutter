package ru.justagod.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskState
import ru.justagod.plugin.data.CutterConfig
import ru.justagod.plugin.data.CutterTaskData

class CutterPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def tasksContainer = project.container(CutterTaskData)
        def config = project.extensions.create("cutter", CutterConfig, tasksContainer)

        config.builds.all { taskData ->
            if (taskData == null) return

            def gradleTask = project.task(taskData.name + 'Build', type: DefaultTask, dependsOn: project.build) {
                group = 'build'
            }
            project.gradle.taskGraph.whenReady {
                if (it.hasTask(gradleTask)) {
                    it.addTaskExecutionListener new TaskExecutionListener() {
                        @Override
                        void beforeExecute(Task task) {
                            if (task == project.jar) {
                                new CutterAction(config.annotation, config.classesDirs, taskData, config.classesCache, project, config.printSidesTree, config.processDependencies, config.deleteAnnotations).action()
                                if (config.processDependencies) project.jar.getMainSpec().getSourcePaths().clear()
                                else project.jar.getMainSpec().getSourcePaths().removeIf {
                                    it instanceof SourceSetOutput
                                }
                                def tmp = project.jar.getMainSpec().getSourcePaths().clone()
                                project.jar.getMainSpec().getSourcePaths().clear()
                                project.jar.from(config.classesCache)
                                tmp.forEach {
                                    project.jar.from(it)
                                }
                                project.jar.version += '-' + taskData.name
                            }
                        }

                        @Override
                        void afterExecute(Task task, TaskState state) {

                        }
                    }
                }
            }
        }
    }

}
