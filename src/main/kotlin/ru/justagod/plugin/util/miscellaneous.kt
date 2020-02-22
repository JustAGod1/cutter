package ru.justagod.plugin.util

fun <T>Collection<T>.intersectsWith(other: Collection<T>): Boolean {
    return this.any { it in other }
}

fun <T>Collection<T>.intersection(other: Collection<T>): List<T> {
    return this.filter { it in other }
}

fun <T>MutableCollection<T>.intersectsWith(other: Collection<T>) {
    this.removeIf { it !in other }
}

