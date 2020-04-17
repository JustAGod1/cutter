package ru.justagod.plugin.processing.pipeline

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.MethodNode
import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.*
import ru.justagod.plugin.data.DynSideMarker
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.util.CutterUtils
import ru.justagod.plugin.processing.model.InvokeClass
import ru.justagod.plugin.processing.model.MethodDesc
import ru.justagod.plugin.processing.model.ProjectModel
import ru.justagod.plugin.util.PrimitivesAdapter
import ru.justagod.plugin.util.intersectsWith

/**
 * Final stage (just before validation)
 */
class CutterMincer(
        private val targetSides: Set<SideName>,
        private val primalSides: Set<SideName>,
        private val markers: List<DynSideMarker>
): SubMincer<ProjectModel, ProjectModel> {
    override fun process(context: WorkerContext<ProjectModel, ProjectModel>): MincerResultType {
        val tree = context.input.sidesTree
        val invokeClass = CutterUtils.findInvokeClass(context.name, context.mincer, context.input)
        if (invokeClass != null) {
            if (!invokeClass.sides.intersectsWith(targetSides)) {
                processInvokeClass(context, invokeClass)
                // I'm not sure that deleting invoke classes is good idea even if we suppose that its won't be used
                return MincerResultType.MODIFIED
            }

        }
        if (!tree.get(context.name.path, primalSides).intersectsWith(targetSides)) return MincerResultType.DELETED
        var modified = false
        val node = context.info!!.node
        val path = context.name.path



        if (node.fields != null) {
            val fieldsIter = node.fields.iterator()
            while (fieldsIter.hasNext()) {
                val field = fieldsIter.next()
                if (!tree.get(path + (field.name + field.desc), primalSides).intersectsWith(targetSides)) {
                    fieldsIter.remove()
                    modified = true
                }
            }
        }

        if (node.methods != null) {
            val methodsIter = node.methods.iterator()
            while (methodsIter.hasNext()) {
                val method = methodsIter.next()
                val methodSides = tree.get(path + (method.name + method.desc), primalSides)
                if (!methodSides.intersectsWith(targetSides)) {
                    if (context.input.lambdaMethods[context.name]?.contains(MethodDesc(method.name, method.desc)) == true) {
                        emptifyMethod(method)
                    } else {
                        methodsIter.remove()
                    }
                    modified = true
                } else {
                    SidlyInstructionsIter.iterateAndTransform(
                            method.instructions,
                            methodSides,
                            markers
                    ) { (insn,sides) ->
                        if (insn is FrameNode || !sides.intersectsWith(targetSides)) {
                            modified = true
                            return@iterateAndTransform false
                        }
                        true
                    }
                }
            }
        }
        return if (modified) MincerResultType.MODIFIED else MincerResultType.SKIPPED
    }


    private fun processInvokeClass(context: WorkerContext<ProjectModel, ProjectModel>, info: InvokeClass) {
        if (info.name == context.name) return
        val node = context.info!!.node
        val implMethod = node.methods
                ?.find { it.name == info.functionalMethod.name && it.desc == info.functionalMethod.desc }
                ?: error("${info.name} says that its impl method is ${info.functionalMethod} but it isn't implemented in ${context.name}")

        emptifyMethod(implMethod)
    }

    private fun emptifyMethod(method: MethodNode) {
        method.instructions.clear()
        val returnType = fetchTypeReference(Type.getReturnType(method.desc).descriptor)
        if (returnType is PrimitiveTypeReference) {
            when (returnType.kind) {
                PrimitiveKind.BOOLEAN,
                PrimitiveKind.BYTE,
                PrimitiveKind.CHAR,
                PrimitiveKind.SHORT,
                PrimitiveKind.INT -> visitInsns(method, Opcodes.ICONST_0, Opcodes.IRETURN)
                PrimitiveKind.VOID -> method.visitInsn(Opcodes.RETURN)
                PrimitiveKind.FLOAT -> visitInsns(method, Opcodes.FCONST_0, Opcodes.FRETURN)
                PrimitiveKind.LONG -> visitInsns(method, Opcodes.LCONST_0, Opcodes.LRETURN)
                PrimitiveKind.DOUBLE -> visitInsns(method, Opcodes.DCONST_0, Opcodes.DRETURN)
            }
        } else if (returnType is ClassTypeReference && PrimitivesAdapter.isWrapper(returnType)) {
            val primitive = PrimitivesAdapter.getPrimitive(returnType)
            if (primitive != PrimitiveKind.VOID) {
                when (primitive) {
                    PrimitiveKind.BOOLEAN,
                    PrimitiveKind.BYTE,
                    PrimitiveKind.CHAR,
                    PrimitiveKind.SHORT,
                    PrimitiveKind.INT -> visitInsns(method, Opcodes.ICONST_0)
                    PrimitiveKind.FLOAT -> visitInsns(method, Opcodes.FCONST_0)
                    PrimitiveKind.LONG -> visitInsns(method, Opcodes.LCONST_0)
                    PrimitiveKind.DOUBLE -> visitInsns(method, Opcodes.DCONST_0)
                }
                PrimitivesAdapter.wrap(method, primitive)
                method.visitInsn(Opcodes.ARETURN)
            }
        } else {
            visitInsns(method, Opcodes.ACONST_NULL, Opcodes.ARETURN)
        }
        method.visitMaxs(10, 10)
    }

    private fun visitInsns(mv: MethodVisitor, vararg opcodes: Int) = opcodes.forEach { mv.visitInsn(it) }

    override fun endProcessing(input: ProjectModel, cache: MincerArchive?, inheritance: InheritanceHelper, pipeline: Pipeline<ProjectModel, ProjectModel>) {
        pipeline.value = input
    }

}