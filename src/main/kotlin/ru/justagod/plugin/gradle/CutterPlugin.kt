package ru.justagod.plugin.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.processing.config.CutterConfig
import ru.justagod.cutter.processing.config.InvokeClass
import ru.justagod.cutter.processing.config.MethodDesc
import ru.justagod.cutter.processing.config.SideName
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Callable

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
        val path = "defaults/kek"
        val input = javaClass.classLoader.getResourceAsStream(path) ?: throw RuntimeException("Defaults.jar not found.")

        val output = FileOutputStream(target)
        input.copyTo(output, 1024 * 5)


        return target
    }


    @Override
    override fun apply(project: Project) {
        project.afterEvaluate {
            this.project = project

            defaultsFile = copyDefaults(project)
            project.gradle.addListener(this)

            initiateTasks(project)
        }

    }

    private fun compileConfiguration(): Configuration {
        return project.configurations.findByName("compile")
            ?: project.configurations.findByName("compileClasspath")!!
    }

    private fun initiateTasks(project: Project) {
        initiateServerTask(project)
        initiateClientTask(project)
    }

    private fun cutterTask(
        name: String,
        project: Project,
    ): CutterTask {
        val task = project.task(mapOf("type" to CutterTask::class.java), name) as CutterTask
        task.classPath.set(listOf(compileConfiguration()))
        task.dependsOn += project.tasks.findByName("build")!!
        task.group = "cutter"

        task.from(Callable { project.tasks.findByName("jar")!!.outputs.files.map { project.zipTree(it) }})

        return task
    }

    private fun initiateClientTask(project: Project) {
        val clientTask = cutterTask("buildClient", project)

        clientTask.description = "Builds jar with only client classes"
        clientTask.config.set(configForSide(clientSide))
        clientTask.classifier = "client"
    }


    private fun initiateServerTask(project: Project) {
        val serverTask = cutterTask("buildServer", project)

        serverTask.config.set(configForSide(serverSide))
        serverTask.description = "Builds jar with only server classes"
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
                ),
                deleteAnnotations = true
            )
        }
    }

    override fun beforeResolve(dependencies: ResolvableDependencies) {
        project.dependencies.add(compileConfiguration().name, project.files(defaultsFile))

        val jar = project.tasks.findByName("jar") as Jar
        jar.from(project.zipTree(defaultsFile))

        project.gradle.removeListener(this)
    }

    override fun afterResolve(dependencies: ResolvableDependencies) {
    }

}
