package ru.justagod.plugin.processing

import org.objectweb.asm.Type
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
import ru.justagod.model.fetchTypeReference
import ru.justagod.plugin.data.SideInfo
import java.lang.IllegalArgumentException

class ModelBuilderMincer(
        private val annotation: ClassTypeReference,
        private val primalSides: List<SideInfo>,
        private val invokesHolder: ClassTypeReference,
        private val invokes: Map<String, List<SideInfo>>,
        private val printSidesTree: Boolean
) : SubMincer<Unit, ProjectModel> {
    override fun process(
            name: ClassTypeReference,
            data: ClassModel?,
            node: ClassNode?,
            pipeline: Pipeline<Unit, ProjectModel>,
            input: Unit,
            inheritance: InheritanceHelper,
            nodes: NodesFactory,
            factory: ModelFactory,
            skipped: Boolean
    ): ProcessingResult {
        data!!
        node!!
        val model = pipeline.value!!
        val path = if (data.name.simpleName == "package-info") data.name.path.dropLast(1) else data.name.path

        processClass(model.sidesTree, inheritance, data, path)
        processFields(model.sidesTree, data, path)
        processMethods(name, model.sidesTree, node, path, inheritance)

        if (name == invokesHolder) processHolder(model, data, invokes)

        return ProcessingResult.NOOP
    }

    private fun processHolder(model: ProjectModel, data: ClassModel, invokes: Map<String, List<SideInfo>>) {
        for ((name, sides) in invokes) {
            val method = data.methods.find { it.name == name }
                    ?: throw IllegalArgumentException("Class ${data.name} must have method with name $name")

            if (!method.access.static) throw IllegalArgumentException("Method $name must be static")
            val args = Type.getArgumentTypes(method.desc)
            if (args.size != 1) throw IllegalArgumentException("Method $name must have 1 argument")
            val argumentType = fetchTypeReference(args[0].descriptor) as? ClassTypeReference
                    ?: throw IllegalArgumentException("Argument of method $name must be class or interface")
            model.invokeClasses[argumentType] = sides
            model.sidesTree.set(argumentType.path, sides.toSet())
        }
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

        return processSides(sides, tree, pathProvider)
    }

    private fun processSides(sides: List<SideInfo>, tree: SidesTree, pathProvider: () -> List<String>): List<SideInfo> {
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

    private fun processMethods(name: ClassTypeReference, tree: SidesTree, node: ClassNode, path: List<String>, inheritance: InheritanceHelper) {
        node.methods?.forEach { method ->
            val resultingSides = arrayListOf<SideInfo>()
            var first = true
            inheritance.walk(name) { clazz ->
                val it = clazz.methods.find { it.name == method.name && it.desc == method.desc } ?: return@walk
                val annotations = it.invisibleAnnotations + it.visibleAnnotations
                val sides = fetchSides(annotations) ?: return@walk
                if (first) {
                    resultingSides.addAll(sides)
                    first = false
                } else {
                    resultingSides.removeIf { it !in sides }
                }
            }
            if (first) return@forEach

            val sides = processSides(resultingSides, tree) { path + (method.name + method.desc) } ?: return@forEach
            for (instruction in method.instructions) {
                if (instruction is TypeInsnNode) {
                    val type = ClassTypeReference(instruction.desc.replace("/", "."))
                    try {
                        if (type.path.any { it.matches("[0-9]+".toRegex()) }) {
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


    override fun startProcessing(input: Unit, cache: List<ClassTypeReference>?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, ProjectModel>) {
        pipeline.value = ProjectModel()
    }

    override fun endProcessing(input: Unit, cache: List<ClassTypeReference>?, inheritance: InheritanceHelper, pipeline: Pipeline<Unit, ProjectModel>) {
        pipeline.value!!.sidesTree.identify(null)
        if (printSidesTree) println(pipeline.value!!.sidesTree.toString(primalSides.toSet()))
    }
}