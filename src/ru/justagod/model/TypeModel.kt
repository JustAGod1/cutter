package ru.justagod.model

sealed class TypeModel(parent: AbstractModel) : AbstractModel(parent) {
    operator fun component1() = parent

    abstract fun getMyType(): TypeReference

}

interface ClassParent {
    val rawType: ClassTypeReference
}

class FinalTypeModel(val type: TypeReference, parent: AbstractModel) : TypeModel(parent), ClassParent {
    override val rawType: ClassTypeReference
        get() = type as ClassTypeReference

    override fun getMyType(): TypeReference = type

    operator fun component2() = type
    override fun toString(): String {
        return "FinalTypeModel(" +
                "type='$type'" +
                ")"
    }


}

sealed class GenericTypeModel(parent: AbstractModel) : TypeModel(parent) {
    private var _myType: TypeReference? = null

    final override fun getMyType(): TypeReference {
        if (_myType == null) {
            _myType = calculateMyType()
        }
        return _myType!!
    }

    abstract fun calculateMyType(): TypeReference
}


class ParameterizedTypeModel(val owner: ClassTypeReference, parametersFetcher: () -> List<TypeModel>, parent: AbstractModel) : TypeModel(parent), ClassParent {

    val parameters: List<TypeModel> by lazy { parametersFetcher() }

    override val rawType: ClassTypeReference
        get() = owner

    override fun toString(): String {
        return "ParameterizedTypeModel(owner='$owner', parameters=$parameters)"
    }

    override fun getMyType(): TypeReference = owner
}

class ReferencedGenericTypeModel(val name: String, val bound: TypeModel?, parent: AbstractModel) : GenericTypeModel(parent) {
    operator fun component2() = name
    override fun toString(): String {
        return "ReferencedGenericTypeModel(name='$name', bound='$bound')"
    }

    override fun calculateMyType(): TypeReference {
        var default = bound?.getMyType() ?: OBJECT_REFERENCE
        if (parent == null) return default
        var myIndex: Int? = null
        if (parent is ClassModel) {
            val parentGeneric = parent.typeParameters.find { it.name == name } ?: error("WTF")
            myIndex = parent.typeParameters.indexOf(parentGeneric)
            default = parentGeneric.bound!!.getMyType()
        }
        if (parent.parent is FieldModel) {
            val parentGeneric = (parent.parent.type as? ParameterizedTypeModel)?.parameters?.getOrNull(myIndex ?: -1)
                    ?: return default
            return parentGeneric.getMyType()
        }
        return default
    }

}

class WildcardGenericTypeModel(val bound: TypeModel, parent: AbstractModel) : GenericTypeModel(parent) {
    operator fun component2() = bound
    override fun toString(): String {
        return "WildcardGenericTypeModel(bound='$bound')"
    }

    override fun calculateMyType(): TypeReference = bound.getMyType()
}

class ArrayGenericTypeModel(val type: TypeModel, parent: AbstractModel) : GenericTypeModel(parent) {
    operator fun component2() = type
    override fun toString(): String {
        return "ArrayGenericTypeModel(type=$type)"
    }

    override fun calculateMyType(): TypeReference {
        return ArrayTypeReference(type.getMyType())
    }

}