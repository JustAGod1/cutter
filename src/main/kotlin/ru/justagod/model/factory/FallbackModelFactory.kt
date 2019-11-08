package ru.justagod.model.factory

import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.AbstractModel
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference

class FallbackModelFactory(loader: ClassLoader, nodes: NodesFactory) : ModelFactory {

    private val bytecodeModelFactory = BytecodeModelFactory(nodes)
    private val reflectionModelFactory = ReflectionModelFactory(loader)

    override fun makeModel(type: ClassTypeReference, parent: AbstractModel?): ClassModel {
        return try {
            bytecodeModelFactory.makeModel(type, parent)
        } catch (e: BytecodeModelFactory.BytecodeNotFoundException) {
            try {
                reflectionModelFactory.makeModel(type, parent)
            } catch (e: Exception) {
                throw RuntimeException("Exception while creating class model via reflection model factory", e)
            }
        } catch (e: Exception) {
            throw RuntimeException("Exception while creating class model via bytecode model factory", e)
        }
    }
}