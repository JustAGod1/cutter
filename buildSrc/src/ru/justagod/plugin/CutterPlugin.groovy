package ru.justagod.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
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
            project.task(task.name + 'Build', type: Jar) {
                from(config.classesCache)
                archiveName = project.jar.archiveName - '.jar' + '-' + task.name + '.jar'
                destinationDir = project.jar.destinationDir
                group = 'build'
                doFirst {
                    new CutterAction(config.annotation, config.classesDirs, task, config.classesCache, project).action()
                }
            }.dependsOn('classes')
        }
    }

}
