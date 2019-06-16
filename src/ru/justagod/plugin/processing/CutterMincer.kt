package ru.justagod.plugin.processing

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.ProcessingResult
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.ClassModel
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.ModelFactory
import ru.justagod.model.fetchTypeReference
import ru.justagod.plugin.data.SideInfo

class CutterMincer(
        private val targetSides: List<SideInfo>,
        private val primalSides: Set<SideInfo>,
        private val invokesClass: ClassTypeReference,
        private val invokes: Map<String, List<SideInfo>>
) : SubMincer<ProjectModel, Unit> {
    override fun process(
            name: ClassTypeReference,
            data: ClassModel?,
            node: ClassNode?,
            pipeline: Pipeline<ProjectModel, Unit>,
            input: ProjectModel,
            inheritance: InheritanceHelper,
            nodes: NodesFactory,
            factory: ModelFactory,
            skipped: Boolean
    ): ProcessingResult {
        val sides = fetchSides(inheritance, input, name)
        val path = name.path
        if (!targetSides.any { it in sides }) {
            println(name.name + " has been deleted")
            return ProcessingResult.DELETE
        }
        var modified = false
        node!!.fields?.removeIf {
            val fieldSides = input.sidesTree.get(path + ("field " + it.name), primalSides)
            val result = !targetSides.any { it in fieldSides }
            if (result) {
                modified = true
                println(name.name + "." + it.name + " has been discarded")
            }
            result
        }

        val lambdas = arrayListOf<String>()
        node.methods?.removeIf { method ->
            val methodPath = if (method.access and Opcodes.ACC_SYNTHETIC != 0 && method.name.startsWith("lambda")) {
                val author = method.name.split("$")[1]
                path + ("$author()")
            } else path + (method.name + method.desc)
            val methodSides = input.sidesTree.get(methodPath, primalSides)
            val result = !targetSides.any { it in methodSides }
            if (result) {
                modified = true
                println(name.name + "." + method.name + method.desc + " has been discarded")
            }
            if (!result) lambdas += analyzeCode(method.instructions, input, inheritance)
            if (method.name !in lambdas) {
                result
            } else {
                println(name.name + "." + method.name + method.desc + " has been discarded")
                modified = true
                true
            }
        }

        return if (modified) ProcessingResult.REWRITE else ProcessingResult.NOOP
    }

    private fun fetchSides(inheritance: InheritanceHelper, model: ProjectModel, name: ClassTypeReference): Set<SideInfo> {
        val tree = model.sidesTree
        val resultingSides = hashSetOf<SideInfo>()
        var first = true
        inheritance.walk(name) {
            val sides = tree.get(it.name.path, primalSides)
            if (first) {
                resultingSides += sides
                first = false
            } else {
                resultingSides.removeIf { it !in sides }
            }
        }
        return if (!first) resultingSides else primalSides
    }

    private fun analyzeCode(instructions: InsnList, model: ProjectModel, inheritance: InheritanceHelper): List<String> {
        val foundedLambdas = arrayListOf<String>()
        val targetClasses = model.invokeClasses.filter { !it.value.any { it in targetSides } }.keys.toSet()
        val iter = instructions.iterator()
        var line = 0
        loop@ while (iter.hasNext()) {
            val node = iter.next()
            when (node) {
                is LineNumberNode -> line = node.line
                is MethodInsnNode -> {
                    val type = fetchTypeReference("L" + node.owner + ";") as ClassTypeReference
                    if (node.opcode == Opcodes.INVOKESTATIC) {
                        if (type == invokesClass) {
                            val sides = invokes[node.name] ?: continue@loop

                            if (!sides.any { it in targetSides }) {
                                iter.remove()
                                println("MethodInsn@$line has been discarded")
                            }
                        }
                    }
                    if (targetClasses.any { inheritance.isChild(type, it) }) {
                        iter.remove()
                        println("MethodInsn@$line has been discarded")
                    }
                }
                is TypeInsnNode -> {
                    val type = fetchTypeReference("L" + node.desc + ";") as ClassTypeReference
                    if (targetClasses.any { inheritance.isChild(type, it) }) {
                        iter.remove()
                        println("TypeInsn@$line has been discarded")
                    }
                }
                is FieldInsnNode -> {
                    val type = fetchTypeReference(node.desc) as? ClassTypeReference ?: continue@loop
                    if (targetClasses.any { inheritance.isChild(type, it) }) {
                        iter.remove()
                        println("FieldInsn@$line has been discarded")
                    }
                }
                is InvokeDynamicInsnNode -> {
                    if (fetchTypeReference(Type.getReturnType(node.desc).descriptor) in targetClasses) {
                        val arg = node.bsmArgs[1].toString()
                        if (arg.indexOf('.') < arg.indexOf('(')) {
                            val name = arg.substringAfter('.').substringBefore('(')
                            foundedLambdas += name
                        }
                        iter.remove()
                        println("DynamicInsn@$line has been discarded")
                    }
                }
            }
        }
        return foundedLambdas
    }
}