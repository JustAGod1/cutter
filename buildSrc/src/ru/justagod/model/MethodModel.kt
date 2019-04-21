package ru.justagod.model

class MethodModel(
        val name: String,
        val access: AccessModel,
        val visibleAnnotations: Map<ClassTypeReference, Map<String, Any>>,
        val invisibleAnnotations: Map<ClassTypeReference, Map<String, Any>>,
        parent: AbstractModel
) : AbstractModel(parent)