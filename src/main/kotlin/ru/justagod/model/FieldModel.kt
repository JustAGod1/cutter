package ru.justagod.model

import org.objectweb.asm.tree.FieldNode
import ru.justagod.model.factory.BytecodeModelFactory


class FieldModel(
    val name: String,
    val type: TypeModel,
    val access: AccessModel,
    val nullable: Boolean,
    val node: FieldNode,
    parent: AbstractModel
) : AbstractModel(parent) {
    override fun toString(): String {
        return "FieldModel(name='$name', type=$type, access=$access)"
    }


    fun hasNullableAnnotation(path: String) =
        node.invisibleTypeAnnotations?.find { it.typePath?.toString() == path }?.desc == BytecodeModelFactory.nullableAnnotation.desc()
}