package ru.justagod.plugin.data

import groovy.transform.CompileStatic
import kotlin.io.ByteStreamsKt
import kotlin.io.FilesKt
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import ru.justagod.plugin.processing.model.InvokeClass

class CutterConfig {

    /**
     * Полное имя аннотации, которую будет искать вырезалка
     * <br>
     * Хороший пример аннотации:
     * <code>
     * <pre>
     * public @interface GradleSideOnly {
     *     GradleSide[] value();
     * }
     * </pre>
     * </code>
     *
     * Либо:
     * <pre>
     * public @interface GradleSideOnly {
     *     GradleSide value();
     * }
     * </pre>
     * </code>
     *
     *
     */
    String annotation

    boolean removeAnnotations = false

    /**
     * Список конфигураций билдов
     * @see #builds(groovy.lang.Closure)
     */
    NamedDomainObjectContainer<CutterTaskData> builds

    List<InvocationClassData> invokes = new ArrayList<>()
    private final Project project

    def invocation(Closure closure) {
        def data = new InvocationClassData()
        invokes.add(data)
        ConfigureUtil.configure(closure, data)
    }

    CutterConfig(NamedDomainObjectContainer<CutterTaskData> builds, Project project) {
        this.project = project
        this.builds = builds
    }

    /**
     * Эта функция нужна исключительно для улучшения читаемости кода, но все сделано так, что без нее никак))
     *
     * На вход вам нужно подать имя энума стороны, который вы используете в заданной аннотации
     * Например для энума
     * <pre>
     *     <code><br>
     * public enum GradleSide {
     *     SERVER, CLIENT
     * }
     *     </code>
     * </pre>
     * Значения могут быть:
     * CLIENT и SERVER
     * @param name имя энума стороны.
     * @return удобавиромое представление стороны
     */
    SideName side(String name) {
        def info = SideName.make$cutter(name)
        return info
    }

    /**
     *
     * @param closure
     * @return
     */
    def builds(Closure closure) {
        builds.configure(closure)
    }

    def builds() {
        return builds
    }

    void initializeDefault() {
        def targetDir = new File(project.file(".gradle"), "cutter-defaults")
        targetDir.mkdirs()

        def names = ['Defaults.jar', 'Defaults-sources.jar', 'Defaults-javadoc.jar']
        for (String name : names) {
            def target = new File(targetDir, name)
            def path = "defaults/" + name
            def input = getClass().classLoader.getResourceAsStream(path)
            if (input == null) throw new RuntimeException("Cannot find $path")
            def output = new FileOutputStream(target)
            ByteStreamsKt.copyTo(input, output, 1024 * 5)

            project.dependencies.compile(project.files(target))
            println("Adding ${target.absolutePath} to compile conf...")
            project.jar.from(target)
        }
        annotation = "ru.justagod.cutter.GradleSideOnly"
        def serverSide = side('SERVER')
        def clientSide = side('CLIENT')
        invocation {
            name = 'ru.justagod.cutter.invoke.InvokeClient'
            sides = [clientSide]
            method = 'run()V'
        }
        invocation {
            name = 'ru.justagod.cutter.invoke.InvokeServer'
            sides = [serverSide]
            method = 'run()V'
        }
        invocation {
            name = 'ru.justagod.cutter.invoke.InvokeServerValue'
            sides = [serverSide]
            method = 'run()Ljava/lang/Object;'
        }
        invocation {
            name = 'ru.justagod.cutter.invoke.InvokeClientValue'
            sides = [clientSide]
            method = 'run()Ljava/lang/Object;'
        }
        builds {
            client {
                targetSides = [clientSide]
                primalSides = [clientSide, serverSide]
            }
            server {
                targetSides = [serverSide]
                primalSides = [clientSide, serverSide]
            }
        }
    }

}
