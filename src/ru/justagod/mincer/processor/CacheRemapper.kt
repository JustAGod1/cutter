package ru.justagod.mincer.processor

import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory
import java.io.File

interface CacheRemapper {

    fun remap(cache: MutableCollection<File>, nodes: NodesFactory, inheritance: InheritanceHelper, factory: ModelFactory)
}