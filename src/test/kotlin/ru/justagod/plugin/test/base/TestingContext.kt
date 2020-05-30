package ru.justagod.plugin.test.base

import java.io.File

abstract class TestingContext {

    abstract fun before()

    // returns path to folder with compiled classes
    abstract fun compileFolder(root: File, conf: String?): File

    fun compileResourceFolder(root: String, name: String?): File {
        val rootFile = resolve(root)
        return compileFolder(rootFile, name)
    }


    companion object {
        fun resolve(resource: String): File {
            val path = if (!resource.startsWith("/")) {
                TestingContext::class.java.getResource("/$resource")!!
            } else {
                TestingContext::class.java.getResource(resource)!!
            }.path

            return File(path)
        }
    }


}