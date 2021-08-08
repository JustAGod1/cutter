package ru.justagod.cutter.model.factory

import ru.justagod.cutter.model.AbstractModel
import ru.justagod.cutter.model.ClassModel
import ru.justagod.cutter.model.ClassTypeReference
import java.util.concurrent.ConcurrentHashMap

class CachedFactory(private val delegate: ModelFactory) : ModelFactory {

    private val cache = ConcurrentHashMap<Pair<ClassTypeReference, AbstractModel?>, ClassModel>()

    override fun makeModel(type: ClassTypeReference, parent: AbstractModel?): ClassModel {
        return cache.computeIfAbsent(Pair(type, parent)) { delegate.makeModel(type, parent) }
    }

    companion object {


    }
}