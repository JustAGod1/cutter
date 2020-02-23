package ru.justagod.plugin.data

import groovy.transform.CompileStatic
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

    /**
     * Список конфигураций билдов
     * @see #builds(groovy.lang.Closure)
     */
    NamedDomainObjectContainer<CutterTaskData> builds

    List<InvocationClassData> invokes = new ArrayList<>()

    def invocation(Closure closure) {
        def data = new InvocationClassData()
        invokes.add(data)
        ConfigureUtil.configure(closure, data)
    }

    CutterConfig(NamedDomainObjectContainer<CutterTaskData> builds) {
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
        def info = new SideName(name)
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

}
