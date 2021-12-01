package ru.justagod.plugin.test.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSetOutput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.*
import org.mockito.Mockito.*
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

        `when`(cutterTask.classPath).thenReturn(mock(ListProperty::class.java) as ListProperty<Configuration>)
        doReturn(mock(ListProperty::class.java)).`when`(cutterTask).classPath
        doReturn(mock(Property::class.java)).`when`(cutterTask).config

        `when`(project.configurations).thenReturn(mock(ConfigurationContainer::class.java))


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
        val classes = mock(SourceSetOutput::class.java)
        val plugin = CutterPlugin()
        CutterPlugin::class.java
            .getDeclaredMethod(methodName, Project::class.java, FileCollection::class.java)
            .also { it.isAccessible = true }
            .invoke(plugin, project, classes)


        assertEquals(config, targetConfig)


    }

    @Test
    fun tasksAdded() {
        `when`(cutterTask.config).thenReturn(mock(Property::class.java) as Property<CutterConfig>)
        val classes = mock(SourceSetOutput::class.java)
        val plugin = CutterPlugin()
        CutterPlugin::class.java
            .getDeclaredMethod("initiateTasks", Project::class.java, FileCollection::class.java)
            .also { it.isAccessible = true }
            .invoke(plugin, project, classes)



        verify(project).task(ArgumentMatchers.eq(mapOf("type" to CutterTask::class.java)), ArgumentMatchers.eq("buildServer"))
        verify(project).task(ArgumentMatchers.eq(mapOf("type" to CutterTask::class.java)), ArgumentMatchers.eq("buildClient"))
    }

}