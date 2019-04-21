package ru.justagod.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.jvm.tasks.Jar
import ru.justagod.plugin.data.CutterConfig
import ru.justagod.plugin.data.CutterTaskData

class CutterPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def tasksContainer = project.container(CutterTaskData)
        def config = project.extensions.create("cutter", CutterConfig, tasksContainer)

        config.builds.all { task ->
            if (task == null) return
            project.task(task.name + 'Build', type: DefaultTask) {
                group = 'build'
                doLast {
                    new CutterAction(config.annotation, config.classesDirs, task, config.classesCache, project, config.printSidesTree, config.processDependencies).action()
                    if (config.processDependencies) project.jar.getMainSpec().getSourcePaths().clear()
                    else project.jar.getMainSpec().getSourcePaths().removeIf { it instanceof SourceSetOutput }
                    project.jar.from(config.classesCache)
                    project.jar.version += '-' + task.name
                    project.jar.execute()
                }
            }.dependsOn('classes')
        }
    }

}
