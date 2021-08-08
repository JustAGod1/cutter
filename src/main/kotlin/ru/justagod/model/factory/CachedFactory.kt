package ru.justagod.model.factory

import ru.justagod.model.AbstractModel
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import java.util.concurrent.ConcurrentHashMap

class CachedFactory(private val delegate: ModelFactory) : ModelFactory {

    private val cache = ConcurrentHashMap<Pair<ClassTypeReference, AbstractModel?>, ClassModel>()

    override fun makeModel(type: ClassTypeReference, parent: AbstractModel?): ClassModel {
        return cache.computeIfAbsent(Pair(type, parent)) { delegate.makeModel(type, parent) }
    }

    companion object {


    }
}