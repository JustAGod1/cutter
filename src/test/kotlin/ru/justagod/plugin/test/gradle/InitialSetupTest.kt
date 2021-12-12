package ru.justagod.plugin.test.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskContainer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.*
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import ru.justagod.cutter.processing.config.CutterConfig
import ru.justagod.plugin.gradle.CutterPlugin
import ru.justagod.plugin.gradle.CutterTask

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class InitialSetupTest {

    @Mock
    lateinit var cutterTask: CutterTask

    @Mock
    lateinit var project: Project


    @BeforeEach
    fun before() {
        MockitoAnnotations.openMocks(this)

        `when`(cutterTask.classPath).thenReturn(mock(ListProperty::class.java) as ListProperty<FileCollection>)
        doReturn(mock(ListProperty::class.java)).`when`(cutterTask).classPath
        doReturn(mock(Property::class.java)).`when`(cutterTask).config

        val configurations = mock(ConfigurationContainer::class.java)
        whenever(configurations.findByName(ArgumentMatchers.eq("compile"))).thenReturn(mock(Configuration::class.java))
        `when`(project.configurations).thenReturn(configurations)

        val tasks = mock(TaskContainer::class.java)
        whenever(tasks.findByName(ArgumentMatchers.eq("build"))).thenReturn(mock(Task::class.java))
        `when`(project.tasks).thenReturn(tasks)

        `when`(project.task(eq(mapOf("type" to CutterTask::class.java)), anyString())).thenAnswer {
            cutterTask
        }

    }

    @Test
    fun validateServerTask() {
        validateTask("initiateServerTask", CutterPlugin.configForSide(CutterPlugin.serverSide))
    }

    @Test
    fun validateClientTask() {
        validateTask("initiateClientTask", CutterPlugin.configForSide(CutterPlugin.clientSide))
    }

    private fun validateTask(methodName: String, targetConfig: CutterConfig) {
        val configProperty = mock(Property::class.java) as Property<CutterConfig>

        var config: CutterConfig? = null
        doAnswer { config = it.getArgument(0, CutterConfig::class.java); null }
            .`when`(configProperty).set(ArgumentMatchers.any() as CutterConfig?)

        `when`(cutterTask.config).thenReturn(configProperty)
        val plugin = CutterPlugin()
        plugin.javaClass.getDeclaredField("project").also { it.isAccessible = true }.set(plugin, project)
        CutterPlugin::class.java
            .getDeclaredMethod(methodName, Project::class.java)
            .also { it.isAccessible = true }
            .invoke(plugin, project)


        assertEquals(config, targetConfig)


    }

    @Test
    fun tasksAdded() {
        `when`(cutterTask.config).thenReturn(mock(Property::class.java) as Property<CutterConfig>)
        val plugin = CutterPlugin()
        plugin.javaClass.getDeclaredField("project").also { it.isAccessible = true }.set(plugin, project)
        CutterPlugin::class.java
            .getDeclaredMethod("initiateTasks", Project::class.java)
            .also { it.isAccessible = true }
            .invoke(plugin, project)



        verify(project).task(
            ArgumentMatchers.eq(mapOf("type" to CutterTask::class.java)),
            ArgumentMatchers.eq("buildServer")
        )
        verify(project).task(
            ArgumentMatchers.eq(mapOf("type" to CutterTask::class.java)),
            ArgumentMatchers.eq("buildClient")
        )
    }

}