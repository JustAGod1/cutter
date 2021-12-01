package ru.justagod.cutter.processing.model

import org.objectweb.asm.tree.MethodNode
import ru.justagod.cutter.model.ClassTypeReference

sealed class ProjectAtom {

    abstract fun path(): String

    abstract fun parent(): ProjectAtom?

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

}

data class ClassAtom(val name: ClassTypeReference): ProjectAtom() {
    override fun path(): String = name.name
    override fun parent(): ProjectAtom = FolderAtom(name.name.substringBeforeLast("."))
}

data class MethodAtom(val owner: ClassTypeReference, val name: String, val desc: String): ProjectAtom() {
    constructor(owner: ClassTypeReference, method: MethodNode): this(owner, method.name, method.desc)

    override fun path(): String = owner.name + "." + name + desc
    override fun parent(): ProjectAtom = ClassAtom(owner)
}
data class FieldAtom(val owner: ClassTypeReference, val name: String): ProjectAtom() {
    override fun path(): String = owner.name + ".field:" + name
    override fun parent(): ProjectAtom = ClassAtom(owner)
}

data class FolderAtom(val path: String): ProjectAtom() {
    override fun path(): String = path
    override fun parent(): FolderAtom? = when {
        path.contains(".") -> FolderAtom(path.substringBeforeLast("."))
        path != "" -> FolderAtom("")
        else -> null
    }
}

