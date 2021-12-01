package ru.justagod.plugin.gradle

import java.io.File

interface CutterProcessor {

    fun process(root: File)

}