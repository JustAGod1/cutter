package ru.justagod.plugin.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.processing.config.CutterConfig
import ru.justagod.cutter.processing.config.InvokeClass
import ru.justagod.cutter.processing.config.MethodDesc
import ru.justagod.cutter.processing.config.SideName
import java.io.File
import java.io.FileOutputStream

class CutterPlugin : Plugin<Project>, DependencyResolutionListener {

    lateinit var project: Project
    lateinit var defaultsFile: File

    init {
        instance = this
    }

    private fun copyDefaults(project: Project): File {
        val targetDir = File(project.file(".gradle"), "cutter-defaults")
        targetDir.mkdirs()
        val name = "Defaults.jar"

        val target = File(targetDir, name)
        val path = "defaults/$name"
        val input = javaClass.classLoader.getResourceAsStream(path) ?: throw RuntimeException("Defaults.jar not found.")

        val output = FileOutputStream(target)
        input.copyTo(output, 1024 * 5)

        return target
    }


    @Override
    override fun apply(project: Project) {
        this.project = project

        defaultsFile = copyDefaults(project)
        project.gradle.addListener(this)

        val classesOutput = project.convention.getPlugin(JavaPluginConvention::class.java)
            .sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)!!
            .output
        initiateTasks(project, classesOutput)

    }

    private fun compileConfiguration(): Configuration {
        return project.configurations.findByName("compile")
            ?: project.configurations.findByName("compileClasspath")!!
    }

    private fun initiateTasks(project: Project, classes: FileCollection) {
        initiateServerTask(project, classes)
        initiateClientTask(project, classes)
    }

    private fun initiateClientTask(project: Project, classes: FileCollection) {
        val clientTask = project.task(mapOf("type" to CutterTask::class.java), "buildClient") as CutterTask
        clientTask.description = "Builds jar with only client classes"
        clientTask.from(classes)
        clientTask.classPath.set(listOf(compileConfiguration()))
        clientTask.config.set(configForSide(clientSide))
        clientTask.from(project.zipTree(defaultsFile))

        clientTask.classifier = "client"
    }
    private fun initiateServerTask(project: Project, classes: FileCollection) {
        val serverTask = project.task(mapOf("type" to CutterTask::class.java), "buildServer") as CutterTask
        serverTask.description = "Builds jar with only server classes"
        serverTask.from(classes)
        serverTask.classPath.set(listOf(compileConfiguration()))
        serverTask.config.set(configForSide(serverSide))
        serverTask.dependsOn += project.tasks.findByName("classes")
        serverTask.from(project.zipTree(defaultsFile))

        serverTask.classifier = "server"
    }




    companion object {
        @JvmStatic
        lateinit var instance: CutterPlugin

        val serverSide = SideName.make("server")
        val clientSide = SideName.make("client")
        val allSides = setOf(serverSide, clientSide)

        fun configForSide(side: SideName): CutterConfig {
            return CutterConfig(
                annotation = ClassTypeReference("ru.justagod.cutter.GradleSideOnly"),
                validationOverrideAnnotation = ClassTypeReference("ru.justagod.cutter.NoValidation"),
                primalSides = allSides,
                targetSides = setOf(side),
                invocators = listOf(
                    InvokeClass(
                        name = ClassTypeReference("ru.justagod.cutter.invoke.InvokeClient"),
                        sides = setOf(clientSide),
                        functionalMethod = MethodDesc("run", "()V")
                    ),
                    InvokeClass(
                        name = ClassTypeReference("ru.justagod.cutter.invoke.InvokeServer"),
                        sides = setOf(serverSide),
                        functionalMethod = MethodDesc("run", "()V")
                    ),
                    InvokeClass(
                        name = ClassTypeReference("ru.justagod.cutter.invoke.InvokeServerValue"),
                        sides = setOf(serverSide),
                        functionalMethod = MethodDesc("run", "()Ljava/lang/Object;")
                    ),
                    InvokeClass(
                        name = ClassTypeReference("ru.justagod.cutter.invoke.InvokeClientValue"),
                        sides = setOf(clientSide),
                        functionalMethod = MethodDesc("run", "()Ljava/lang/Object;")
                    )
                )
            )
        }
    }

    override fun beforeResolve(dependencies: ResolvableDependencies) {
        for (configuration in project.configurations) {
            println(configuration)
        }
        project.dependencies.add(compileConfiguration().name, project.files(defaultsFile))
        project.gradle.removeListener(this)
    }

    override fun afterResolve(dependencies: ResolvableDependencies) {
    }

}
