package ru.justagod.utils

@Suppress("UNCHECKED_CAST")
fun <T>Any?.cast() = this as T

fun <T>Collection<T>.containsAny(other: Collection<T>): Boolean {
    if (this.size < other.size) {
        return this.any { it in other }
    } else {
        return other.any { it in this }
    }
}