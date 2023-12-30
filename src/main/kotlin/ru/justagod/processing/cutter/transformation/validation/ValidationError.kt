package ru.justagod.processing.cutter.transformation.validation

import ru.justagod.model.ClassTypeReference
import ru.justagod.processing.cutter.config.SideName

sealed class Location(val clazz: ClassTypeReference, val source: String?) {

    abstract fun humanReadable(): String

    protected fun printClickableLocation(methodName: String, lineNumber: Int)
    = "${clazz.name}.$methodName(${source ?: "unknown"}:$lineNumber)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Location

        if (clazz != other.clazz) return false
        if (source != other.source) return false

        return true
    }

    override fun hashCode(): Int {
        var result = clazz.hashCode()
        result = 31 * result + (source?.hashCode() ?: 0)
        return result
    }


}

class FieldLocation(owner: ClassTypeReference, source: String?, val name: String): Location(owner, source) {
    override fun humanReadable(): String = printClickableLocation(name, 0)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as FieldLocation

        return name == other.name
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }


}

class MethodBodyLocation(owner: ClassTypeReference, source: String?, val name: String, val lineNumber: Int)
    : Location(owner, source) {
    override fun humanReadable(): String = printClickableLocation(name, lineNumber)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MethodBodyLocation

        if (name != other.name) return false
        if (lineNumber != other.lineNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + lineNumber
        return result
    }


}

class MethodDescLocation(owner: ClassTypeReference, source: String?, val name: String, val lineNumber: Int)
    : Location(owner, source) {
    override fun humanReadable(): String = printClickableLocation(name, lineNumber)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MethodDescLocation

        if (name != other.name) return false
        if (lineNumber != other.lineNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + lineNumber
        return result
    }


}

class ClassDescLocation(owner: ClassTypeReference, source: String?)
    : Location(owner, source) {
    override fun humanReadable(): String = printClickableLocation("decs", 0)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }


}

sealed class ValidationError(val location: Location, val hisSides: Set<SideName>, val note: String? = null) {

    override fun toString(): String {
        return "${location.humanReadable()}\n" +
                "\t${error()}\n" +
                "\tHis sides: ${if (hisSides.isNotEmpty()) hisSides.joinToString() else "none"}\n" +
                "\tLocation type: ${location.javaClass.simpleName}" +
                if (note != null) "\n\tNote: $note" else ""
    }

    abstract fun error() : String
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValidationError

        if (location != other.location) return false
        if (hisSides != other.hisSides) return false
        if (note != other.note) return false

        return true
    }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + hisSides.hashCode()
        result = 31 * result + (note?.hashCode() ?: 0)
        return result
    }


}

class ClassNotFoundValidationError(
    val clazz: ClassTypeReference,
    location: Location,
    hisSides: Set<SideName>,
    note: String? = null
) : ValidationError(location, hisSides, note) {
    override fun error(): String = "Cannot find class ${clazz.name}"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as ClassNotFoundValidationError

        return clazz == other.clazz
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + clazz.hashCode()
        return result
    }


}

class MethodNotFoundValidationError(
    val owner: ClassTypeReference,
    val name: String,
    val desc: String,
    location: Location,
    hisSides: Set<SideName>,
    note: String? = null
) : ValidationError(location, hisSides, note) {
    override fun error(): String = "Cannot find method ${owner.name}.$name$desc"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MethodNotFoundValidationError

        if (owner != other.owner) return false
        if (name != other.name) return false
        if (desc != other.desc) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + desc.hashCode()
        return result
    }


}

class FieldNotFoundValidationError(
    val owner: ClassTypeReference,
    val name: String,
    location: Location,
    hisSides: Set<SideName>,
    note: String? = null
) : ValidationError(location, hisSides, note) {
    override fun error(): String = "Cannot find field ${owner.name}.$name"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as FieldNotFoundValidationError

        if (owner != other.owner) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }


}


