package ru.justagod.plugin.test.base

interface TestRunner {

    val name: String

    fun run(context: TestingContext): Boolean
}