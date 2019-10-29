package ru.justagod.model

import java.lang.reflect.Modifier

class AccessModel(val access: Int) {

    val public = access and Modifier.PUBLIC != 0
    val private = access and Modifier.PRIVATE != 0
    val protected = access and Modifier.PROTECTED != 0
    val packagePrivate = !(public || private || protected)

    val final = access and Modifier.FINAL != 0
    val static = access and Modifier.STATIC != 0

    val transient = access and Modifier.TRANSIENT != 0
    val volatile = access and Modifier.VOLATILE != 0
    val synchronized = access and Modifier.SYNCHRONIZED != 0
    val isInterface = access and Modifier.INTERFACE != 0
    val abstract = access and Modifier.ABSTRACT

    override fun toString(): String {
        val builder = StringBuilder()
        builder += "["
        builder += when {
            public -> "public"
            private -> "private"
            protected -> "protected"
            else -> "package-private"
        }
        if (final) builder += " final"
        if (static) builder += " static"
        if (transient) builder += " transient"
        if (volatile) builder += " volatile"
        if (synchronized) builder += " synchronized"
        builder += "]"
        return builder.toString()
    }

    private operator fun StringBuilder.plusAssign(v: Any) {
        this.append(v)
    }

}