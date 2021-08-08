package ru.justagod.cutter.mincer.processor

import ru.justagod.cutter.mincer.util.NodesFactory
import ru.justagod.cutter.model.InheritanceHelper
import ru.justagod.cutter.model.factory.ModelFactory
import java.io.File

interface CacheRemapper {

    fun remap(cache: MutableCollection<File>, nodes: NodesFactory, inheritance: InheritanceHelper, factory: ModelFactory)
}