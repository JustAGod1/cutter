package ru.justagod.model

import ru.justagod.model.factory.ModelFactory
import java.lang.Exception
import java.util.*

class InheritanceHelper(private val modelFactory: ModelFactory) {

    private val buffer = HashMap<ClassTypeReference, ClassModel>()
    private val nodes = HashMap<ClassTypeReference, InheritanceNode>()

    fun isChild(child: ClassTypeReference, parent: ClassTypeReference, considerInterfaces: Boolean? = null): Boolean {
        val childNode = getNode(child) ?: return false
        if (childNode.name == parent) return true
        val resolvedChildNode = resolveNode(childNode) ?: return false
        if (considerInterfaces != false && (considerInterfaces == true || getNode(parent)?.isInterface ?: return false)) {
            val result = resolvedChildNode.interfaces.any { isChild(it.name, parent, considerInterfaces) }
            if (result) return true
        }
        if (resolvedChildNode.superClass == null) return false
        return isChild(resolvedChildNode.superClass.name, parent, considerInterfaces)
    }

    inline fun walk(type: ClassTypeReference, acceptor: (ClassModel) -> Unit) {
        val classes = getSuperClasses(type)
        for (clazz in classes) {
            acceptor(clazz)
        }
    }

    fun getSuperClasses(type: ClassTypeReference, target: MutableList<ClassModel> = LinkedList()): List<ClassModel> {
        target.add(getModel(type) ?: return target)
        val node = resolveNode(getNode(type) ?: return target) ?: return target
        node.superClass?.let { getSuperClasses(it.name, target) }
        for (inter in node.interfaces) {
            getSuperClasses(inter.name, target)
        }
        return target
    }

    private fun getNode(type: ClassTypeReference): InheritanceNode? {
        if (type in nodes) return nodes[type]!!
        val node = makeNode(getModel(type) ?: return null)
        nodes[type] = node
        return node
    }

    private fun makeNode(model: ClassModel) = InheritanceNode(model.access.isInterface, model.name, model)

    private fun resolveNode(node: InheritanceNode): ResolvedInheritanceNode? {
        if (node is ResolvedInheritanceNode) return node
        val interfaces = node.model.interfaces.map { getNode(it.rawType) ?: return null }
        val superClass = node.model.superClass?.let { getNode(it.rawType) }
        val resolvedNode = ResolvedInheritanceNode(node, interfaces, superClass)
        nodes[node.name] = resolvedNode
        return resolvedNode
    }

    private fun getModel(type: ClassTypeReference): ClassModel? {
        if (type in buffer) return buffer[type]!!
        val model = try { modelFactory.makeModel(type, null) } catch (e: Exception) { null }
        buffer[type] = model ?: return null
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