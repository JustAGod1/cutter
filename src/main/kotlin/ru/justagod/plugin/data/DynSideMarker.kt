package ru.justagod.plugin.data

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.util.CutterUtils
import java.lang.RuntimeException

abstract class DynSideMarker {
    abstract fun getSides(value: AbstractInsnNode, conditionalOpcode: Int, primalSides: Set<SideName>) : Set<SideName>?
}

class FieldDynSideMarker(
        private val owner: ClassTypeReference,
        private val name: String,
        private val targetSides: Set<SideName>
) : DynSideMarker() {


    override fun getSides(value: AbstractInsnNode, conditionalOpcode: Int, primalSides: Set<SideName>): Set<SideName>? {
        if (value is FieldInsnNode) {
            if (value.owner == owner.internalName() && value.name == name) {
                if (value.desc != "Z") throw RuntimeException("${owner.name}.$name has to be boolean")
                if (conditionalOpcode == Opcodes.IFEQ) return targetSides
                else if (conditionalOpcode == Opcodes.IFNE) return primalSides - targetSides
                else {
                    println("Okay. You're trying to use ${CutterUtils.opcodeToString(conditionalOpcode)} on boolean")
                    println("Actually I have no idea what does it means so I'll just pretend that it's not my case")
                    println("Consider report it to JustAGod. Thanks in advance.")
                }
            }
        }
        return null
    }
}

class MethodDynSideMarker(
        private val owner: ClassTypeReference,
        private val name: String,
        private val desc: String,
        private val targetSides: Set<SideName>
) : DynSideMarker() {

    override fun getSides(value: AbstractInsnNode, conditionalOpcode: Int, primalSides: Set<SideName>): Set<SideName>? {
        if (value is MethodInsnNode) {
            if (value.owner == owner.internalName() && value.name == name && value.desc == desc) {
                if (Type.getReturnType(value.desc).descriptor != "Z")
                    throw RuntimeException("${owner.name}.$name$desc has to return boolean")
                if (conditionalOpcode == Opcodes.IFEQ) return targetSides
                else if (conditionalOpcode == Opcodes.IFNE) return primalSides - targetSides
                else {
                    println("Okay. You're trying to use ${CutterUtils.opcodeToString(conditionalOpcode)} on boolean")
                    println("Actually I have no idea what does it means so I'll just pretend that it's not my case")
                    println("Consider report it to JustAGod. Thanks in advance.")
                }
            }
        }
        return null
    }

}

class DynSideMarkerBuilder {
    fun field() = DynSideMarkerBuilderField()
    fun method() = DynSideMarkerBuilderMethod()
}
class DynSideMarkerBuilderField {

    var owner: String? = null
    var name: String? = null
    var sides: Set<SideName>? = null

    fun owner(owner: String): DynSideMarkerBuilderField {
        this.owner = owner
        return this
    }

    fun name(name: String): DynSideMarkerBuilderField {
        this.name = name
        return this
    }

    fun sides(sides: Set<SideName>): DynSideMarkerBuilderField {
        this.sides = sides
        return this
    }

    fun build(): FieldDynSideMarker {
        return FieldDynSideMarker(
                ClassTypeReference(owner ?: error("owner is not defined")),
                name ?: error("name is not defined"),
                sides ?: error("sides is not defined")
        )
    }

}
class DynSideMarkerBuilderMethod {

    var owner: String? = null
    var name: String? = null
    var desc: String? = null
    var sides: Set<SideName>? = null

    fun owner(owner: String): DynSideMarkerBuilderMethod {
        this.owner = owner
        return this
    }

    fun name(name: String): DynSideMarkerBuilderMethod {
        this.name = name
        return this
    }

    fun desc(desc: String): DynSideMarkerBuilderMethod {
        this.desc = desc
        return this
    }

    fun sides(sides: Set<SideName>): DynSideMarkerBuilderMethod {
        this.sides = sides
        return this
    }

    fun build(): MethodDynSideMarker {
        return MethodDynSideMarker(
                ClassTypeReference(owner ?: error("owner is not defined")),
                name ?: error("name is not defined"),
                desc ?: error("desc is not defined"),
                sides ?: error("sides is not defined")
        )
    }
}


