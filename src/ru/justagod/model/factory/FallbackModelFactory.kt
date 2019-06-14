package ru.justagod.model.factory

import ru.justagod.model.AbstractModel
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference

class FallbackModelFactory(loader: ClassLoader, harvester: (String) -> ByteArray?) : ModelFactory {

    private val bytecodeModelFactory = BytecodeModelFactory(harvester)
    private val reflectionModelFactory = ReflectionModelFactory(loader)

    override fun makeModel(type: ClassTypeReference, parent: AbstractModel?): ClassModel {
        return try {
            bytecodeModelFactory.makeModel(type, parent)
        } catch (e: BytecodeModelFactory.BytecodeNotFoundException) {
            try {
                reflectionModelFactory.makeModel(type, parent)
            } catch (e: Exception) {
                throw RuntimeException("Exception while creating class model via reflection model factory (may be ${type.name})", e)
            }
        } catch (e: Exception) {
            throw RuntimeException("Exception while creating class model via bytecode model factory (may be ${type.name})", e)
        }
    }
}