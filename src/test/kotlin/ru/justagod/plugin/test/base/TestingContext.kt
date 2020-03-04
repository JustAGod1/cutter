package ru.justagod.plugin.test.base

import ru.justagod.plugin.data.SideName
import java.io.File
import java.net.URL

abstract class TestingContext {

    abstract fun before()

    // returns path to folder with compiled classes
    abstract fun compileFolder(root: File, name: String): File

    fun compileResourceFolder(root: String, name: String): File {
        val rootFile = resolve(root)
        return compileFolder(rootFile, name)
    }


    protected fun resolve(resource: String): File {
        val path = if (!resource.startsWith("/")) {
            TestingContext::class.java.getResource("/$resource")!!
        } else {
            TestingContext::class.java.getResource(resource)!!
        }.path

        return File(path)
    }


}