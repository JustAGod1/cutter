package ru.justagod.processing.cutter.transformation.validation

import ru.justagod.model.ClassTypeReference
import ru.justagod.processing.cutter.config.SideName

sealed class Location(val clazz: ClassTypeReference, val source: String?) {

    abstract fun humanReadable(): String

    protected fun printClickableLocation(methodName: String, lineNumber: Int)
    = "${clazz.name}.$methodName(${source ?: "unknown"}:$lineNumber)"

}

class FieldLocation(owner: ClassTypeReference, source: String?, val name: String): Location(owner, source) {
    override fun humanReadable(): String = printClickableLocation(name, 0)
}

class MethodBodyLocation(owner: ClassTypeReference, source: String?, val name: String, val lineNumber: Int)
    : Location(owner, source) {
    override fun humanReadable(): String = printClickableLocation(name, lineNumber)
}

class MethodDescLocation(owner: ClassTypeReference, source: String?, val name: String, val lineNumber: Int)
    : Location(owner, source) {
    override fun humanReadable(): String = printClickableLocation(name, lineNumber)
}

class ClassDescLocation(owner: ClassTypeReference, source: String?)
    : Location(owner, source) {
    override fun humanReadable(): String = printClickableLocation("decs", 0)
}

sealed class ValidationError(val location: Location, val hisSides: Set<SideName>, val note: String?) {

    override fun toString(): String {
        return "${location.humanReadable()}\n" +
                "\t${error()}\n" +
                "\tHis sides: [$hisSides]\n" +
                "\tLocation type: ${location.javaClass.simpleName}" +
                if (note != null) "\n\tNote: $note" else ""
    }

    abstract fun error() : String

}

class ClassNotFoundValidationError(
    val clazz: ClassTypeReference,
    location: Location,
    hisSides: Set<SideName>,
    note: String?
) : ValidationError(location, hisSides, note) {
    override fun error(): String = "Cannot find class ${clazz.name}"
}

class MethodNotFoundValidationError(
    val owner: ClassTypeReference,
    val name: String,
    val desc: String,
    location: Location,
    hisSides: Set<SideName>,
    note: String?
) : ValidationError(location, hisSides, note) {
    override fun error(): String = "Cannot find method ${owner.name}.$name$desc"
}

class FieldNotFoundValidationError(
    val owner: ClassTypeReference,
    val name: String,
    location: Location,
    hisSides: Set<SideName>,
    note: String?
) : ValidationError(location, hisSides, note) {
    override fun error(): String = "Cannot find field ${owner.name}.$name"
}


