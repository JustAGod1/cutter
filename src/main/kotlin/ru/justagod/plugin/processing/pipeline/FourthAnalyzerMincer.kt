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
import ru.justagod.plugin.data.DynSideMarker
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.model.MethodDesc
import ru.justagod.plugin.processing.model.PathHelper
import ru.justagod.plugin.processing.model.ProjectModel
import ru.justagod.plugin.util.CutterUtils
import ru.justagod.plugin.util.intersection

/**
 * In this stage we analyze lambdas and anonymous classes and assign them the same sides that assigned to
 * methods in which they were defined
 *
 * For example
 * ```java
 * class Foo {
 *   @SideOnly(Side.SERVER)
 *   void bar() {
 *     Runnable d = () -> {};
 *   }
 * }
 * ```
 * Our goal to eliminate synthetic method that will be generated by javac and assign to server side to it
 *
 * Another one
 * ```java
 * class Bar {
 *   @SideOnly(Side.SERVER)
 *   void foo() {
 *     Runnable d = new Runnable() {
 *       void run() {
 *       }
 *     }
 *   }
 * }
 * ```
 * Our goal to eliminate anonymous class that will be generated by javac and assign server side to it
 */
class FourthAnalyzerMincer(
        private val primalSides: Set<SideName>,
        private val markers: List<DynSideMarker>
) : SubMincer<ProjectModel, ProjectModel> {
    override fun process(context: WorkerContext<ProjectModel, ProjectModel>): MincerResultType {
        val node = context.info!!.node
        node.methods?.forEach {
            val sides = context.input.sidesTree.get(PathHelper.method(context.name, it.name, it.desc), primalSides)
            val iter = SidlyInstructionsIter(
                    it.instructions.iterator(),
                    sides,
                    markers
            )
            for ((instruction, sides) in iter) {
                if (instruction is InvokeDynamicInsnNode) {
                    analyzeInvokeDynamic(instruction, context, sides)
                } else if (instruction is TypeInsnNode) {
                    analyzeTypeInsn(instruction, context, sides)
                }
            }
        }

        return MincerResultType.SKIPPED
    }

    private fun analyzeTypeInsn(
            instruction: TypeInsnNode,
            context: WorkerContext<ProjectModel, ProjectModel>,
            sides: Set<SideName>
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
                    sides
            )
        }
    }

    private fun analyzeInvokeDynamic(
            instruction: InvokeDynamicInsnNode,
            context: WorkerContext<ProjectModel, ProjectModel>,
            sides: Set<SideName>
    ) {
        if (instruction.bsm.owner == "java/lang/invoke/LambdaMetafactory") {
            if (instruction.bsm.name != "metafactory" && instruction.bsm.name == "altMetafactory") error("WTF")
            if (instruction.bsm.name == "altMetafactory") {
                val serializable = (instruction.bsmArgs[3] as Int) and 1 != 0
                if (serializable) {
                    println("ОСУЖДАЮ")
                }
            }
            val targetInterface = ClassTypeReference.fromDesc(Type.getReturnType(instruction.desc))
            val invokeClass = CutterUtils.findInvokeClass(
                    targetInterface,
                    context.mincer,
                    context.input
            )
            val implMethod = instruction.bsmArgs[1] as Handle
            if (implMethod.owner == context.name.name.replace('.', '/')) {
                val lambdaMethod = context.info!!.node.methods
                        .find { it.name == implMethod.name && it.desc == implMethod.desc }!!

                // In case when lambda class is one of the invoke classes we can be sure that the
                // instantiated method of the lambda is body of lambda. Even if it isn't so, it's
                // problem of user.
                if (lambdaMethod.access and Opcodes.ACC_SYNTHETIC != 0 || invokeClass != null) {
                    // This method is synthetic so probably it represents lambda's body
                    if (invokeClass != null) {
                        context.input.lambdaMethods.computeIfAbsent(context.name) { hashSetOf() }
                                .add(MethodDesc(implMethod.name, implMethod.desc))
                    }
                    context.input.sidesTree.set(
                            context.name.path + (implMethod.name + implMethod.desc),
                            sides.let {
                                if (invokeClass != null) {
                                    it.intersection(invokeClass.sides).toSet()
                                } else it
                            }
                    )
                }
            }
        }
    }

    override fun endProcessing(input: ProjectModel, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<ProjectModel, ProjectModel>) {
        input.sidesTree.identify(null)
        pipeline.value = input
        if (System.getProperty("print-sides") == "true") println(pipeline.value!!.sidesTree.toString(primalSides.toSet()))
    }
}