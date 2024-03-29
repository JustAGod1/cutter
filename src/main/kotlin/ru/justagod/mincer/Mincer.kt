package ru.justagod.mincer

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.control.MincerResult
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.pipeline.MincerPipelineController
import ru.justagod.mincer.util.NodesFactory
import ru.justagod.model.ClassTypeReference
import ru.justagod.model.InheritanceHelper
import ru.justagod.model.OBJECT_REFERENCE
import ru.justagod.model.factory.BytecodeModelFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class Mincer private constructor(
    val fs: MincerFS,
    queues: List<MincerPipelineController<*>>
) {

    val debug = System.getProperty("mincer-debug") == "true"

    private val queues = queues.toMutableList()
    private val nodes = NodesFactory(this::harvestBytecode)
    val factory = BytecodeModelFactory(nodes)
    val inheritance = InheritanceHelper(factory)

    private val rwClassLocks = hashMapOf<ClassTypeReference, ReentrantReadWriteLock>()

    init {
        queues.forEach { it.start() }
    }


    private val cache = ConcurrentHashMap<String, ByteArray>()

    fun makeNode(name: ClassTypeReference, flags: Int = 0) : ClassNode {
        return readLock(name) {
            nodes.makeNode(name, flags)
        }
    }

    @Synchronized
    fun <T> writeLock(name: ClassTypeReference, block: () -> T): T {
        val lock = rwClassLocks.computeIfAbsent(name) { ReentrantReadWriteLock() }

        val result = lock.write {
            block()
        }

        cleanUpLock(name, lock)

        return result
    }


    @Synchronized
    private fun <T> readLock(name: ClassTypeReference, block: () -> T) : T {
        val lock = rwClassLocks.computeIfAbsent(name) { ReentrantReadWriteLock() }

        val result = lock.read {
            block()
        }
        cleanUpLock(name, lock)

        return result
    }

    private fun cleanUpLock(name: ClassTypeReference, lock: ReentrantReadWriteLock) {
        if (!lock.isWriteLocked && lock.readLockCount <= 0) {
            rwClassLocks -= name
        }
    }

    private fun harvestBytecode(name: String): ByteArray {
        val cached = cache[name]
        if (cached != null) return cached

        var empty = false
        for (i in 1..40) {
            val b = fs.pullClass(name)
            if (b == null) break
            // в этом кейсе кеш не работает и если его починить то сломается весь сталкрафт потому что инвалидацию забыли
            // UPD: коммент не прав, но что тут происходит все еще не ясно
            else if (b.isNotEmpty()) return b
            else empty = true

            Thread.sleep(1)
        }
        if (empty) throw java.lang.RuntimeException("Some multithreading cringe, @justago please fix it.")
        val bytes = this.javaClass.classLoader.getResourceAsStream(name)?.readBytes()
            ?: ClassLoader.getSystemResourceAsStream(name)?.readBytes()
            ?: throw BytecodeModelFactory.BytecodeNotFoundException(name)

        cache[name] = bytes

        return bytes
    }

    fun targetClasses(): ArrayList<ClassTypeReference>? {
        val result = arrayListOf<ClassTypeReference>()

        for (queue in queues) {
            result += queue.targetClass() ?: return null
        }

        return result
    }

    fun advance(name: ClassTypeReference): MincerResult {
        var acc = MincerResult(this, name, null, MincerResultType.SKIPPED)
        for (i in queues.indices) {
            acc = acc merge queues[i].process(this, acc.resultedNode, name)
            if (acc.type == MincerResultType.DELETED) return acc
        }

        return acc
    }

    fun endIteration(): Boolean {
        queues.removeIf { !it.advance() }
        return queues.isNotEmpty()
    }

    fun nodeToBytes(node: ClassNode): ByteArray {
        try {
            val writer = GentleClassWriter()
            node.accept(writer)
            return writer.toByteArray()
        } catch (e: Exception) {
            throw RuntimeException("Exception while writing class " + ClassTypeReference.fromInternal(node.name), e)
        }
    }

    private inner class GentleClassWriter : ClassWriter(COMPUTE_FRAMES or COMPUTE_MAXS) {

        override fun getCommonSuperClass(type1: String, type2: String): String {
            try {
                val reference1 = ClassTypeReference(type1.replace("[/\\\\]".toRegex(), "."))
                val reference2 = ClassTypeReference(type2.replace("[/\\\\]".toRegex(), "."))
                if (reference1 == OBJECT_REFERENCE || reference2 == OBJECT_REFERENCE) return "java/lang/Object"
                val model1 = factory.makeModel(reference1, null)
                val model2 = factory.makeModel(reference2, null)

                if (model1.access.isInterface || model2.access.isInterface) return "java/lang/Object"

                val supers1 = inheritance.getSuperClasses(reference1)
                val supers2 = inheritance.getSuperClasses(reference2)

                for (classModel in supers1) {
                    if (supers2.any { it.name == classModel.name }) return classModel.name.toASMType().internalName
                }
                return "java/lang/Object"
            } catch (e: Exception) {
                return "java/lang/Object"
            }
        }

    }

    class Builder(private val fs: MincerFS) {
        private val registry = LinkedList<MincerPipelineController<*>>()

        fun registerPipeline(pipeline: MincerPipelineController<*>): Builder {
            registry += pipeline
            return this
        }

        fun registerPipelines(pipelines: Collection<MincerPipelineController<*>>): Builder {
            pipelines.forEach { registerPipeline(it) }
            return this
        }


        fun build(): Mincer = Mincer(fs, registry)
    }
}