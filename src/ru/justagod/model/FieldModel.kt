package ru.justagod.model

class FieldModel(
        val name: String,
        val type: TypeModel,
        val access: AccessModel,
        val nullable: Boolean,
        val visibleAnnotations: Map<ClassTypeReference, Map<String, Any>>,
        val invisibleAnnotations: Map<ClassTypeReference, Map<String, Any>>,
        parent: AbstractModel
) : AbstractModel(parent) {
    override fun toString(): String {
        return "FieldModel(name='$name', type=$type, access=$access)"
    }
}