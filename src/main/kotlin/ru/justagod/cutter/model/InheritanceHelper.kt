package ru.justagod.cutter.model

import ru.justagod.cutter.model.factory.BytecodeModelFactory
import ru.justagod.cutter.model.factory.ModelFactory
import java.util.*

class InheritanceHelper(private val modelFactory: ModelFactory) {

    private val buffer = HashMap<ClassTypeReference, ClassModel?>()
    private val nodes = HashMap<ClassTypeReference, InheritanceNode?>()

    @Synchronized
    fun isChild(child: ClassTypeReference, parent: ClassTypeReference): Boolean {
        return isChild0(child, parent)
    }

    private fun isChild0(child: ClassTypeReference, parent: ClassTypeReference): Boolean {
        val childNode = getNode(child) ?: return false
        if (childNode.name == parent) return true
        val resolvedChildNode = resolveNode(childNode)
        if ((getNode(parent) ?: return false).isInterface) {
            val result = resolvedChildNode.interfaces.any { isChild0(it.name, parent) }
            if (result) return true
        }
        if (resolvedChildNode.superClass == null) return false
        return isChild0(resolvedChildNode.superClass.name, parent)
    }

    inline fun walk(type: ClassTypeReference, acceptor: (ClassModel) -> Unit) {
        val classes = getSuperClasses(type)
        for (clazz in classes) {
            acceptor(clazz)
        }
    }

    @Synchronized
    fun getSuperClasses(type: ClassTypeReference, target: MutableList<ClassModel> = LinkedList()): List<ClassModel> {
        return getSuperClasses0(type, target)
    }

    private fun getSuperClasses0(type: ClassTypeReference, target: MutableList<ClassModel> = LinkedList()): List<ClassModel> {
        target.add(getModel(type)!!)
        val node = resolveNode(getNode(type)!!)
        node.superClass?.let { getSuperClasses0(it.name, target) }
        for (inter in node.interfaces) {
            getSuperClasses0(inter.name, target)
        }
        return target
    }

    private fun getNode(type: ClassTypeReference): InheritanceNode? {
        if (type in nodes) return nodes[type]
        val node = getModel(type)?.let { makeNode(it) }
        nodes[type] = node
        return node
    }

    private fun makeNode(model: ClassModel) = InheritanceNode(model.access.isInterface, model.name, model)

    private fun resolveNode(node: InheritanceNode): ResolvedInheritanceNode {
        if (node is ResolvedInheritanceNode) return node
        val interfaces = node.model.interfaces.mapNotNull { getNode(it.rawType) }
        val superClass = node.model.superClass?.let { getNode(it.rawType) }
        val resolvedNode = ResolvedInheritanceNode(node, interfaces, superClass)
        nodes[node.name] = resolvedNode
        return resolvedNode
    }

    private fun getModel(type: ClassTypeReference): ClassModel? {
        if (type in buffer) return buffer[type]
        val model = try {
            modelFactory.makeModel(type, null)
        } catch (e: BytecodeModelFactory.BytecodeNotFoundException) {
            null
        }
        buffer[type] = model
        return model
    }


    private open class InheritanceNode(
            val isInterface: Boolean,
            val name: ClassTypeReference,
            val model: ClassModel
    )

    private class ResolvedInheritanceNode(
            isInterface: Boolean,
            name: ClassTypeReference,
            model: ClassModel,
            val interfaces: List<InheritanceNode>,
            val superClass: InheritanceNode?
    ) : InheritanceNode(isInterface, name, model) {
        constructor(node: InheritanceNode, interfaces: List<InheritanceNode>, superClass: InheritanceNode?)
                : this(node.isInterface, node.name, node.model, interfaces, superClass)
    }
}