package ru.justagod.plugin.data

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.file.pattern.PatternMatcherFactory
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.util.ConfigureUtil
import ru.justagod.plugin.data.SideName.Companion.make
import java.io.File
import java.io.FileOutputStream
import java.util.*

open class CutterConfig(val builds: NamedDomainObjectContainer<CutterTaskData>, private val project: Project) {
    var annotation: String? = null
    var removeAnnotations = false
    var withoutDefaultLib = false
    var validation = true
    var validationOverriderAnnotation: String? = null
    val excludes = arrayListOf<Spec<RelativePath>>()

    var invokes: MutableList<InvocationClassData> = ArrayList()
    fun invocation(closure: InvocationClassData.() -> Unit) {
        val data = InvocationClassData()
        invokes.add(data)
        data.closure()
    }
    fun invocation(closure: Closure<InvocationClassData>?) {
        val data = InvocationClassData()
        invokes.add(data)
        ConfigureUtil.configure(closure, data)
    }


    var markers: MutableList<DynSideMarker> = arrayListOf()

    fun markers(closure: MarkersSpec.() -> Unit) {
        val spec = MarkersSpec()
        spec.closure()
    }

    fun markers(closure: Closure<*>) {
        val spec = MarkersSpec()
        ConfigureUtil.configure(closure, spec)
    }

    fun side(name: String?): SideName {
        return make(name!!)
    }

    fun builds(closure: Closure<*>?) {
        builds.configure(closure)
    }

    fun builds(closure: NamedDomainObjectContainer<CutterTaskData>.() -> Unit) {
        builds.closure()
    }

    fun builds(): NamedDomainObjectContainer<CutterTaskData> {
        return builds
    }


    fun exclude(vararg patterns: String?) {
        if (project.gradle.gradleVersion >= "6")
            error("Excludes aren't supported in gradle 6+ yet. Your version: ${project.gradle.gradleVersion}")
        for (pattern in patterns) {
            excludes.add(PatternMatcherFactory.getPatternMatcher(true, true, pattern))
        }
    }

    fun initializeDefault() {
        initializeDefault(!withoutDefaultLib, false)
    }

    fun initializeDefault(defaultLib: Boolean, heuristic: Boolean) {
        val targetDir = File(project.file(".gradle"), "cutter-defaults")
        targetDir.mkdirs()
        val names = Arrays.asList("Defaults.jar", "Defaults-sources.jar", "Defaults-javadoc.jar")
        for (name in names) {
            val target = File(targetDir, name)
            val path = "defaults/$name"
            val input = javaClass.classLoader.getResourceAsStream(path) ?: throw RuntimeException("Cannot find $path")
            val output = FileOutputStream(target)
            input.copyTo(output, 1024 * 5)
            project.dependencies.add("compile", project.files(target))
            // I'm sorry for that
            if (name == "Defaults.jar" && defaultLib) (project.tasks.getByName("jar") as AbstractArchiveTask).from(project.zipTree(target))
        }
        annotation = "ru.justagod.cutter.GradleSideOnly"
        val serverSide = side ("SERVER")
        val clientSide = side ("CLIENT")
        invocation {
            name = "ru.justagod.cutter.invoke.InvokeClient"
            sides = listOf(clientSide)
            method = "run()V"
        }
        invocation {
            name = "ru.justagod.cutter.invoke.InvokeServer"
            sides = listOf(serverSide)
            method = "run()V"
        }
        invocation {
            name = "ru.justagod.cutter.invoke.InvokeServerValue"
            sides = listOf(serverSide)
            method = "run()Ljava/lang/Object;"
        }
        invocation {
            name = "ru.justagod.cutter.invoke.InvokeClientValue"
            sides = listOf(clientSide)
            method = "run()Ljava/lang/Object;"
        }
        builds {
            create("client") {
                it.targetSides = listOf(clientSide)
                it.primalSides = listOf(clientSide, serverSide)
            }
            create("server") {
                it.targetSides = listOf(serverSide)
                it.primalSides = listOf(clientSide, serverSide)
            }
        }
        if (heuristic) {
            markers {
                field {
                    name = "isRemote"
                    owner = "net.minecraft.world.World"
                    sides = setOf(clientSide)
                }
            }
        }

    }

    inner class MarkersSpec internal constructor() {

        fun field(block: DynSideMarkerBuilderField.() -> Unit) {
            val builder = DynSideMarkerBuilderField()
            builder.block()
            markers.add(builder.build())
        }

        fun field(closure: Closure<*>) {
            field { ConfigureUtil.configure(closure, this) }
        }

        fun method(block: DynSideMarkerBuilderMethod.() -> Unit) {
            val builder = DynSideMarkerBuilderMethod()
            builder.block()
            markers.add(builder.build())
        }

        fun method(closure: Closure<*>) {
            method { ConfigureUtil.configure(closure, this) }
        }
    }
}