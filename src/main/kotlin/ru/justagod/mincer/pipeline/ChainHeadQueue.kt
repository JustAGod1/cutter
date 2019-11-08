package ru.justagod.mincer.pipeline

import org.objectweb.asm.ClassWriter
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.control.MincerArchive
import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.control.MincerResult
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.ClassInfo
import ru.justagod.mincer.processor.SubMincer
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.factory.BytecodeModelFactory

class ChainHeadQueue(
        val pipeline: Pipeline<*, *>,
        val input: Any,
        val archive: MincerArchive?,
        val next: RawChainSegment?
) {

    fun advance(input: Any, fs: MincerFS, inheritance: InheritanceHelper): ChainHeadQueue? {
        if (next == null) return null
        val archive = fs.pullArchive(next.pipeline.id)
        (next.pipeline.worker as SubMincer<Any, Any>).startProcessing(
                input,
                archive,
                inheritance,
                next.pipeline as Pipeline<Any, Any>

        )
        return ChainHeadQueue(next.pipeline, input, archive, next.next)
    }

    fun process(mincer: Mincer, bytecode: ByteArray, lastModificationTime: Long, name: String): MincerResult {
        try {
            if (archive != null && mincer.canSkip && pipeline.skippable) {
                if (lastModificationTime < archive.creationTime)
                    if (parseName(name) in archive.members)
                        return doProcess(mincer, bytecode, name, true)
            }
            if (checkValidity(mincer, name, lastModificationTime))
                return doProcess(mincer, bytecode, name, false)
            return MincerResult(bytecode, MincerResultType.SKIPPED)
        } catch (e: Exception) {
            throw RuntimeException("Exception while processing $name", e)
        }

    }

    private fun checkValidity(mincer: Mincer, name: String, lastModificationTime: Long): Boolean {
        val reference = parseName(name)
        if (archive != null && archive.creationTime > lastModificationTime && reference in archive.members)
            return true
        try {
            val model = {
                mincer.factory.makeModel(
                        reference,
                        null
                )
            }
            return pipeline.filter.isValid(reference, model, mincer.inheritance, mincer.factory)
        } catch (e: Exception) {
            return false
        }
    }

    private fun doProcess(mincer: Mincer, bytecode: ByteArray, name: String, skipped: Boolean): MincerResult {
        val reference = parseName(name)
        val info = if (!skipped) {
            val node = mincer.nodes.makeNode(reference)
            val model = BytecodeModelFactory.makeModel(node, null)
            ClassInfo(model, node)
        } else null
        mincer.submitArchiveEntry(pipeline.id, reference.name)
        val result = (pipeline.worker as SubMincer<Any, Any>)
                .process(
                        WorkerContext(
                                reference,
                                info,
                                pipeline as Pipeline<Any, Any>,
                                input,
                                mincer
                        )
                )
        if (result == MincerResultType.MODIFIED) {
            val writer = GentleClassWriter(mincer)
            try {
                info?.node?.accept(writer)
            } catch (e: Exception) {
                throw RuntimeException("Exception while rewriting node ${reference.name} after ${pipeline.id}", e)
            }
            return MincerResult(writer.toByteArray(), MincerResultType.MODIFIED)
        } else return MincerResult(bytecode, result)
    }

    private fun parseName(name: String): ClassTypeReference {
        return if (name.endsWith(".class"))
            ClassTypeReference(name.dropLast(6).replace('/', '.').replace('\\', '.'))
        else
            ClassTypeReference(name.replace('/', '.').replace('\\', '.'))
    }


    private inner class GentleClassWriter(private val mincer: Mincer) : ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS) {

        override fun getCommonSuperClass(type1: String, type2: String): String {
            try {
                val reference1 = ClassTypeReference(type1.replace("[/\\\\]".toRegex(), "."))
                val reference2 = ClassTypeReference(type2.replace("[/\\\\]".toRegex(), "."))
                val model1 = mincer.factory.makeModel(reference1, null)
                val model2 = mincer.factory.makeModel(reference2, null)

                if (model1.access.isInterface || model2.access.isInterface) return "java/lang/Object"

                val supers1 = mincer.inheritance.getSuperClasses(reference1)
                val supers2 = mincer.inheritance.getSuperClasses(reference2)

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