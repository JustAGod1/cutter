package ru.justagod.model

abstract class ClassModel(
        val name: ClassTypeReference,
        val visibleAnnotations: Map<ClassTypeReference, Map<String, Any>>,
        val invisibleAnnotations: Map<ClassTypeReference, Map<String, Any>>,
        val hasDefaultConstructor: Boolean,
        val enum: Boolean,
        parent: AbstractModel?
) : AbstractModel(parent) {
    abstract val typeParameters: List<ReferencedGenericTypeModel>
    abstract val access: AccessModel
    abstract val fields: List<FieldModel>
    abstract val methods: List<MethodModel>
    abstract val superClass: ClassParent?
    abstract val interfaces: List<ClassParent>

    override fun toString(): String {
        return "ClassModel(name=$name, visibleAnnotations=$visibleAnnotations, invisibleAnnotations=$invisibleAnnotations, hasDefaultConstructor=$hasDefaultConstructor, enum=$enum, typeParameters=$typeParameters, access=$access, fields=$fields, methods=$methods, superClass=$superClass, interfaces=$interfaces)"
    }


}