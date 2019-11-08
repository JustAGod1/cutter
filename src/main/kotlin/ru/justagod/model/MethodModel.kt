package ru.justagod.model

class MethodModel(
        val name: String,
        val access: AccessModel,
        val returnType: TypeModel,
        val parameters: List<TypeModel>,
        parent: AbstractModel
) : AbstractModel(parent)