package ru.justagod.plugin.processing

import ru.justagod.model.factory.BytecodeModelFactory.Companion.toAnnotationsInfo
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.TypeInsnNode
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.ProcessingResult
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.BytecodeModelFactory
import ru.justagod.model.factory.ModelFactory
import ru.justagod.plugin.data.SideInfo

class ModelBuilderMincer(
        private val annotation: ClassTypeReference,
        private val primalSides: List<SideInfo>,
        private val printSidesTree: Boolean
) : SubMincer<Unit, SidesTree> {
    override fun process(
            name: ClassTypeReference,
            data: ClassModel?,
            node: ClassNode?,
            pipeline: Pipeline<Unit, SidesTree>,
            input: Unit,
            inheritance: InheritanceHelper,
            nodes: NodesFactory,
            factory: ModelFactory,
            skipped: Boolean
    ): ProcessingResult {
        data!!
        node!!
        val tree = pipeline.value!!
        val path = if (data.name.simpleName == "package-info") data.name.path.dropLast(1) else data.name.path

        processClass(tree, inheritance, data, path)
        processFields(tree, data, path)
        processMethods(tree, node, path)

        return ProcessingResult.NOOP
    }

    private fun processClass(tree: SidesTree, inheritance: InheritanceHelper, data: ClassModel, path: List<String>) {
        val resultingSides = hashSetOf<SideInfo>()
        var first = true
        inheritance.walk(data.name) {
            val annotations = it.invisibleAnnotations + it.visibleAnnotations
            val sides = fetchSides(annotations) ?: return@walk
            if (first) {
                resultingSides += sides
                first = false
            } else {
                resultingSides.removeIf { it !in sides }
            }
        }
        if (!first) tree.set(path, resultingSides.toSet())
    }

    private fun fetchSides(annotations: Map<ClassTypeReference, Map<String, Any>>): List<SideInfo>? {
        if (annotation in annotations) {
            return (annotations[annotation]?.get("value") as? BytecodeModelFactory.EnumHolder)?.value?.map { SideInfo(it) }
        } else return null
    }

    private fun proccessAnnotations(
            annotations: Map<ClassTypeReference, Map<String, Any>>,
            tree: SidesTree,
            pathProvider: () -> List<String>
    ): List<SideInfo>? {
        val sides = fetchSides(annotations) ?: return null

        val path = pathProvider()
        tree.set(path, sides.toSet())
        return sides
    }

    private fun processFields(tree: SidesTree, data: ClassModel, path: List<String>) {
        for (field in data.fields) {
            val annotations = field.invisibleAnnotations + field.visibleAnnotations
            proccessAnnotations(annotations, tree) { path + ("field " + field.name) }
        }
    }

    private fun processMethods(tree: SidesTree, node: ClassNode, path: List<String>) {
        node.methods?.forEach { method ->
            val annotations = (method.invisibleAnnotations?.toAnnotationsInfo() ?: emptyMap()).plus(method.visibleAnnotations?.toAnnotationsInfo() ?: emptyMap())
            val sides = proccessAnnotations(annotations, tree) { path + (method.name + "()") } ?: return@forEach
            for (instruction in method.instructions) {
                if (instruction is TypeInsnNode) {
                    val type = ClassTypeReference(instruction.desc.replace("/", "."))
                    try {
                        if (type.path.any { it.matches("[0-9]+".toRegex()) } ) {
                            val newPath = path + type.path.last()
                            tree.set(newPath, sides.toSet())
                        }
                    } catch (e: Exception) {
                        // Похуй
                    }
                }
            }
        }
    }



    override fun startProcessing(input: Unit, cache: List<ClassTypeReference>?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, SidesTree>) {
        pipeline.value = SidesTree("root")
    }

    override fun endProcessing(input: Unit, cache: List<ClassTypeReference>?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, SidesTree>) {
        pipeline.value!!.identify(null)
        if (printSidesTree) println(pipeline.value!!.toString(primalSides.toSet()))
    }
}