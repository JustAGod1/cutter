package ru.justagod.plugin.processing.pipeline

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.ProjectModel

class FourthAnalyzerMincer(private val primalSides: Set<SideName>) : SubMincer<ProjectModel, ProjectModel> {
    override fun process(context: WorkerContext<ProjectModel, ProjectModel>): MincerResultType {
        val node = context.info!!.node
        node.methods?.forEach {
            for (instruction in it.instructions) {
                if (instruction is InvokeDynamicInsnNode) {
                    analyzeInvokeDynamic(instruction, context, it)
                } else if (instruction is TypeInsnNode) {
                    analyzeTypeInsn(instruction, context, it)
                }
            }
        }

        return MincerResultType.SKIPPED
    }

    private fun analyzeTypeInsn(
            instruction: TypeInsnNode,
            context: WorkerContext<ProjectModel, ProjectModel>,
            method: MethodNode
    ) {

        val type = ClassTypeReference(Type.getObjectType(instruction.desc).internalName.replace('/', '.'))

        try {
            context.mincer.nodes.makeNode(type)
        } catch (e: Exception) {
            return
        }
        if (type.name.startsWith(context.name.name) && type.path.drop(context.name.path.size).any { it.toIntOrNull() != null }) {
            // So probably this class is anonymous
            context.input.sidesTree.set(
                    type.path,
                    context.input.sidesTree.get(
                            context.name.path + (method.name + method.desc),
                            primalSides
                    )
            )
        }
    }

    private fun analyzeInvokeDynamic(
            instruction: InvokeDynamicInsnNode,
            context: WorkerContext<ProjectModel, ProjectModel>,
            method: MethodNode
    ) {
        if (instruction.bsm.owner == "java/lang/invoke/LambdaMetafactory") {
            if (instruction.bsm.name != "metafactory" && instruction.bsm.name == "altMetafactory") error("WTF")
            if (instruction.bsm.name == "altMetafactory") {
                val serializable = (instruction.bsmArgs[3] as Int) and 1 != 0
                if (serializable) {
                    println("ОСУЖДАЮ")
                }
            }
            val implMethod = instruction.bsmArgs[1] as Handle
            if (implMethod.owner == context.name.name.replace('.', '/')) {
                val lambdaMethod = context.info!!.node.methods
                        .find { it.name == implMethod.name && it.desc == implMethod.desc }!!

                if (lambdaMethod.access and Opcodes.ACC_SYNTHETIC != 0) {
                    // This method is synthetic so probably it represents lambda's body
                    context.input.sidesTree.set(
                            context.name.path + (implMethod.name + implMethod.desc),
                            context.input.sidesTree.get(
                                    context.name.path + (method.name + method.desc),
                                    primalSides
                            )
                    )
                }
            }
        }
    }

    override fun endProcessing(input: ProjectModel, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<ProjectModel, ProjectModel>) {
        input.sidesTree.identify(null)
        pipeline.value = input
        println(pipeline.value!!.sidesTree.toString(primalSides.toSet()))
    }
}