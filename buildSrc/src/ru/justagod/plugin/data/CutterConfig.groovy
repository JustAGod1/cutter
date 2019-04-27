package ru.justagod.plugin.data

import org.gradle.api.NamedDomainObjectContainer

class CutterConfig {
    /**
     * Удлять ли заданную аннотацию из всех возможных мест после билда
     * @see #annotation
     */
    boolean deleteAnnotations = false
    /**
     * Обрабатывать ли зависимости на равне с кодом
     * Если да поведение такого:
     * 1) Автовырезалка скопирует все ваши классы из {@link #classesDirs} и классы ваших зависимостей в {@link #classesCache}
     *    При том ваши классы будут иметь приоритет при дубликатах
     * 2) Обработает все классы, вырезав лишние как из вашего кода, так и из зависимостей
     * 3) Собирет жарник исключительно из классов в {@link #classesCache}
     *
     * Если нет поведение такого:
     * 1) Скопирует все ваши классы из {@link #classesDirs} в {@link #classesCache}
     * 2) Обработает все ваши классы
     * 3) Собирет жарник согласно настройкам jar
     */
    boolean processDependencies = true

    /**
     * Выводить ли дерево классов во время обработки
     * Нужно чтобы было легко понять, что у вас вырезается, а что остается
     */
    boolean printSidesTree = false

    /**
     * Папка куда будут скопированы ваши классы из {@link #classesDirs} и, возможно, классы зависимостей
     *
     * Нужно чтобы не ломать инкриментальную компиляцию
     *
     * @see #processDependencies
     */
    File classesCache

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
     * Если ее не будет в класс пазе ничего страшного не произойдет, но лучше выставить {@link #deleteAnnotations} в true
     */
    String annotation

    /**
     * Список конфигураций билдов
     * @see #builds(groovy.lang.Closure)
     */
    NamedDomainObjectContainer<CutterTaskData> builds
    /**
     * Место откуда будут скопированы классы для последующей обработки и сборки
     * Нужно из-за того, что котлин, скала, груви и пр. могут иметь отдельную папку классов
     *
     * Если вы используете исключительно жаву, вам будет достаточно написать что-то типа {@code compileJava.destinationDir}
     */
    List<File> classesDirs

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
    SideInfo side(String name) {
        def info = new SideInfo(name)
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
