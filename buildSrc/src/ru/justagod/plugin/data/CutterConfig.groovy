package ru.justagod.plugin.data

import org.gradle.api.NamedDomainObjectContainer

class CutterConfig {
    boolean printSidesTree = false
    File classesCache
    String annotation
    private List<SideInfo> sides = []
    NamedDomainObjectContainer<CutterTaskData> builds
    List<File> classesDirs

    CutterConfig(NamedDomainObjectContainer<CutterTaskData> builds) {
        this.builds = builds
    }

    SideInfo side(String name) {
        def info = new SideInfo(name)
        sides += info
        return info
    }

    def builds(Closure closure) {
        builds.configure(closure)
    }

    def builds() {
        return builds
    }
}
