package ru.justagod.plugin.processing.pipeline.validation.data

import ru.justagod.model.ClassTypeReference

sealed class ValidationError(val holder: ClassTypeReference, val name: String, val src: String?, val line: Int) {
    abstract fun description(): String
    override fun toString(): String = "${description()}\n    at ${holder.name}.$name(${src ?: "unknown"}:$line)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValidationError

        if (holder != other.holder) return false
        if (name != other.name) return false
        if (src != other.src) return false
        if (line != other.line) return false

        return true
    }

    override fun hashCode(): Int {
        var result = holder.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (src?.hashCode() ?: 0)
        result = 31 * result + line
        return result
    }


}

class ClassError(val subject: ClassTypeReference, holder: ClassTypeReference, name: String, src: String?, line: Int)
    : ValidationError(holder, name, src, line) {
    override fun description(): String = "Cannot find class ${subject.name}"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as ClassError

        if (subject != other.subject) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + subject.hashCode()
        return result
    }


}

class MethodError(val methodHolder: ClassTypeReference, val methodName: String, val methodDesc: String, holder: ClassTypeReference, name: String, src: String?, line: Int)
    : ValidationError(holder, name, src, line) {
    override fun description(): String = "Cannot find method ${methodHolder.name}.$methodName$methodDesc"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MethodError

        if (methodHolder != other.methodHolder) return false
        if (methodName != other.methodName) return false
        if (methodDesc != other.methodDesc) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + methodHolder.hashCode()
        result = 31 * result + methodName.hashCode()
        result = 31 * result + methodDesc.hashCode()
        return result
    }


}

class FieldError(val fieldHolder: ClassTypeReference, val fieldName: String, holder: ClassTypeReference, name: String, src: String?, line: Int)
    : ValidationError(holder, name, src, line) {
    override fun description(): String = "Cannot find field ${fieldHolder.name}.$fieldName"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as FieldError

        if (fieldHolder != other.fieldHolder) return false
        if (fieldName != other.fieldName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fieldHolder.hashCode()
        result = 31 * result + fieldName.hashCode()
        return result
    }


}



