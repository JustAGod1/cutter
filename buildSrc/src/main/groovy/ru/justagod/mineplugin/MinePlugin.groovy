package ru.justagod.mineplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Created by JustAGod on 01.03.2018.
 */
class MinePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        makeTask(project, "buildClient", GradleSide.CLIENT)
        makeTask(project, "buildServer", GradleSide.SERVER)
    }

    private static void makeTask(Project project, String name, GradleSide side) {
        Task task = project.task(name)
        task.getActions().add(new TransformationAction(side))
        task.dependsOn "build"
        task.group = "build"
    }
}
