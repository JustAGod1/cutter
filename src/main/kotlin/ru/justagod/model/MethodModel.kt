package ru.justagod.model

import org.objectweb.asm.tree.MethodNode

class MethodModel(
        val name: String,
        val access: AccessModel,
        val returnType: TypeModel,
        val parameters: List<TypeModel>,
        val node: MethodNode,
        parent: AbstractModel
) : AbstractModel(parent)