package ru.justagod.plugin.test.base.runner.trivial

class ImageModel private constructor() {
    val root = Directory("", null)

    fun getMandatoryEntries(configName: String): Set<ProjectEntry> {
        return parseDirectory(root, configName)
    }

    private fun parseDirectory(directory: Directory, confName: String): Set<ProjectEntry> {
        val result = directory.directories.values.filter { it.doesExistInConf(confName) }.flatMap { parseDirectory(it, confName) }.toMutableSet()
        result += directory.classes.values.filter { it.doesExistInConf(confName) }.flatMap { parseClass(it, confName) }.toMutableSet()
        if (directory.name != "" && directory.doesExistInConf(confName)) result += directory
        return result
    }

    private fun parseClass(klass: Klass, confName: String): Set<ProjectEntry> {
        return (klass.fields.values.filter { it.doesExistInConf(confName) } + klass.methods.flatMap { it.value }.filter { it.doesExistInConf(confName) }).toHashSet().also { if (klass.doesExistInConf(confName)) it += klass }
    }

    companion object {
        fun make(configure: Directory.() -> Unit): ImageModel {
            val image = ImageModel()
            image.root.configure()
            return image
        }
    }


    abstract class ProjectEntry(protected val parent: ProjectEntry?) {
        private var shouldExists: ((String) -> Boolean) = { true }

        fun conf(confName: String): ProjectEntry {
            shouldExists = { it == confName }
            return this
        }

        fun conf(vararg confName: String): ProjectEntry {
            shouldExists = { it in confName }
            return this
        }

        fun existsWhen(block: (String) -> Boolean): ProjectEntry {
            shouldExists = block
            return this
        }

        fun doesExistInConf(confName: String) = shouldExists!!(confName)
    }

    class Directory(val name: String, parent: ProjectEntry?) : ProjectEntry(parent) {

        val directories = hashMapOf<String, Directory>()
        val classes = hashMapOf<String, Klass>()

        override fun toString(): String {
            if (name == "") return ""

            val ps = parent.toString()
            return if (ps.isEmpty()) name
            else "$parent.$name"
        }

        fun dir(name: String): ProjectEntry = dir(name) {}
        fun dir(name: String, configure: Directory.() -> Unit): ProjectEntry {
            val dir = Directory(name, this)
            directories[name] = dir
            dir.configure()
            return dir
        }

        fun klass(name: String): ProjectEntry = klass(name) {}
        fun klass(name: String, configure: Klass.() -> Unit): ProjectEntry {
            val klass = Klass(name, this)
            classes[name] = klass
            klass.configure()
            return klass
        }

    }

    class Klass(val name: String, parent: ProjectEntry) : ProjectEntry(parent) {

        val methods = hashMapOf<String, MutableList<MethodInfo>>()
        val fields = hashMapOf<String, FieldInfo>()

        override fun toString(): String {
            return "$parent.$name"
        }

        fun method(name: String, desc: String): ProjectEntry {
            val method = MethodInfo(name, desc, this)
            methods.computeIfAbsent(name) { arrayListOf() } += method
            return method
        }

        fun field(name: String, desc: String): ProjectEntry {
            val field = FieldInfo(name, desc, this)
            fields[name] = field
            return field
        }
    }

    class MethodInfo(val name: String, val desc: String, parent: ProjectEntry) : ProjectEntry(parent) {
        override fun toString(): String {
            return "$parent.$name: $desc"
        }
    }

    class FieldInfo(val name: String, val desc: String, parent: ProjectEntry) : ProjectEntry(parent) {
        override fun toString(): String {
            return "$parent.$name$desc"
        }
    }


}