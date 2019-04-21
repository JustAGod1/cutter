package ru.justagod.mincer.context

import ru.justagod.mincer.pipeline.Pipeline
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.util.NodesFactory
import org.objectweb.asm.ClassWriter
import ru.justagod.mincer.processor.ProcessingResult
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.BytecodeModelFactory
import ru.justagod.model.factory.ModelFactory
import java.io.File

abstract class MincerContext(
        protected val factory: ModelFactory,
        protected val inheritance: InheritanceHelper,
        protected val nodes: NodesFactory,
        protected val root: File
) {

    @Suppress("UNCHECKED_CAST")
    protected fun accept(file: File, mincer: Pipeline<*, *>, value: Any, skipped: Boolean) {
        val node = if (!skipped) nodes.makeNode(file) else null
        val model = if (!skipped) BytecodeModelFactory.makeModel(node!!, null) else null
        val result = (mincer.worker as SubMincer<Any, Any>)
                        .process(
                                ClassTypeReference(file.path.drop(root.absolutePath.length + 1).dropLast(6).replace("[/\\\\]".toRegex(), ".")),
                                model,
                                node,
                                mincer as Pipeline<Any, Any>,
                                value,
                                inheritance,
                                nodes,
                                factory,
                                skipped
                        )
        if (result == ProcessingResult.REWRITE) {
            val writer = GentleClassWriter()
            node?.accept(writer)
            file.writeBytes(writer.toByteArray())
        } else if (result == ProcessingResult.DELETE) {
            file.delete()
        }
    }

    private inner class GentleClassWriter : ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS) {

        override fun getCommonSuperClass(type1: String, type2: String): String {
            try {
                val reference1 = ClassTypeReference(type1.replace("[/\\\\]".toRegex(), "."))
                val reference2 = ClassTypeReference(type2.replace("[/\\\\]".toRegex(), "."))
                val model1 = factory.makeModel(reference1, null)
                val model2 = factory.makeModel(reference2, null)

                if (model1.access.isInterface || model2.access.isInterface) return "java/lang/Object"

                val supers1 = inheritance.getSuperClasses(reference1)
                val supers2 = inheritance.getSuperClasses(reference2)

                for (model in supers1) {
                    if (supers2.any { it.name == model.name }) return model.name.toASMType().internalName
                }
                return "java/lang/Object"
            } catch (e: Exception) {
                return "java/lang/Object"
            }
        }

    }
}