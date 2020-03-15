package ru.justagod.model.factory

import ru.justagod.model.*
import org.jetbrains.annotations.Nullable
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl
import java.lang.reflect.*
import org.objectweb.asm.Type as AsmType

class ReflectionModelFactory(val loader: ClassLoader) : ModelFactory {

    private val antiRecursionBuffer = arrayListOf<Pair<Type, TypeModel>>()

    override fun makeModel(type: ClassTypeReference, parent: AbstractModel?): ClassModel {
        val clazz = Class.forName(type.name, false, loader)
        val access = AccessModel(clazz.modifiers)
        val hasDefaultConstructor = clazz.constructors.any { it.parameters.isEmpty() }
        val classModel = InternalClassModel(
                parent,
                emptyMap(),
                emptyMap(),
                hasDefaultConstructor,
                clazz.isEnum,
                access,
                ClassTypeReference(clazz.name)
        )
        val fields = clazz.declaredFields.map { makeFieldModel(it, classModel) }
        val typeParameters = clazz.typeParameters.map { makeTypeModel(it, classModel) }
        val methods = emptyList<MethodModel>()
        classModel._superClass = if (clazz.superclass != null)
            makeTypeModel(clazz.genericSuperclass ?: clazz.superclass, classModel) as ClassParent
        else null
        classModel._interfaces = clazz.genericInterfaces.map { makeTypeModel(it, classModel) } as List<ClassParent>
        classModel._fields = fields
        classModel._methods = methods
        classModel._typeParameters = { typeParameters.filterIsInstance<ReferencedGenericTypeModel>() }
        return classModel
    }

    private fun makeFieldModel(field: Field, parent: AbstractModel): FieldModel {
        val name = field.name
        val access = AccessModel(field.modifiers)
        val type = makeTypeModel(field.genericType, parent)

        return FieldModel(name, type, access, field.getAnnotation(Nullable::class.java) != null, parent)
    }

    private fun makeTypeModel(type: Type, parent: AbstractModel): TypeModel {
        val buffered = antiRecursionBuffer.find { it.first === type }
        if (buffered != null) {
            return buffered.second
        }
        return when (type) {
            is Class<*> -> put(type, FinalTypeModel(fetchTypeReference(AsmType.getDescriptor(type)), parent))
            is GenericArrayType -> put(type, ArrayGenericTypeModel(makeTypeModel(type.genericComponentType, parent), parent))
            is ParameterizedType -> {
                val model = put(type, ParameterizedTypeModel(
                        fetchTypeReference(AsmType.getDescriptor(type.rawType as Class<*>)) as ClassTypeReference,
                        { type.actualTypeArguments.map { put(it, makeTypeModel(it, parent)) } },
                        parent
                ))

                model
            }
            is TypeVariableImpl<*> -> put(type, ReferencedGenericTypeModel(type.name, makeTypeModel(type.bounds[0], parent), parent))
            is WildcardType -> put(type, WildcardGenericTypeModel(makeTypeModel(type.upperBounds[0], parent), parent))
            else -> error("")
        }
    }

    private fun <T : TypeModel> put(type: Type, model: T): T {
        antiRecursionBuffer += type to model

        return model
    }

}