package ru.justagod.model.factory

import ru.justagod.model.*

@Suppress("PropertyName")
class InternalClassModel(
        parent: AbstractModel?,
        visibleAnnotations: Map<ClassTypeReference, Map<String, Any>>,
        invisibleAnnotations: Map<ClassTypeReference, Map<String, Any>>,
        hasDefaultConstructor: Boolean,
        enum: Boolean,
        override val access: AccessModel,
        name: ClassTypeReference
) : ClassModel(name, visibleAnnotations, invisibleAnnotations, hasDefaultConstructor, enum, parent) {

    var _superClass: ClassParent? = null
    override val superClass: ClassParent?
        get() = _superClass
    var _interfaces: List<ClassParent>? = null
    override val interfaces: List<ClassParent>
        get() = _interfaces!!
    var _typeParameters: (() -> List<ReferencedGenericTypeModel>)? = null
    override val typeParameters: List<ReferencedGenericTypeModel>
        get() = _typeParameters!!()
    var _fields: List<FieldModel>? = null
    override val fields: List<FieldModel>
        get() = _fields!!
    var _methods: List<MethodModel>? = null
    override val methods: List<MethodModel>
        get() = _methods!!
}