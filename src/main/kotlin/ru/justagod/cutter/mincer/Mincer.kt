package ru.justagod.cutter.mincer

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import ru.justagod.cutter.mincer.control.MincerFS
import ru.justagod.cutter.mincer.control.MincerResult
import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.pipeline.MincerPipelineController
import ru.justagod.cutter.mincer.util.NodesFactory
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.model.InheritanceHelper
import ru.justagod.cutter.model.OBJECT_REFERENCE
import ru.justagod.cutter.model.factory.BytecodeModelFactory
import ru.justagod.cutter.model.factory.CachedFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write


/**
 * Mincer makes it easy to process and analyze compiled classes
 *
 * You can create Mincer instance via [Builder]
 *
 * Imagine you constructed Mincer somehow. Now you can process some bytecode:
 * ```kotlin
 * do {
 *      advance(className).onModification {
 *          f.writeBytes(it)
 *      }.onDeletion {
 *          f.delete()
 *      }
 * } while(mincer.endIteration())
 * ```
 *
 * So you just need to pass class names in mincer class path and then write resulted bytecode or delete class if
 * mincer decides to delete class
 *
 * To know what mincer do when you call advance or endIteration you need to know that:
 * Mincer has the following mandatory components:
 * 1. Pipelines
 * 2. Class path
 *
 * Pipelines is constructed versions of [ru.justagod.cutter.mincer.pipeline.MincerPipeline].
 * Where's pipeline in every moment of its lifecycle has only one active sub mincer, mincer may have several active pipelines.
 * In such case bytecode passed to each active pipeline one by one.
 *
 * Class path is classes that mincer can obtain through passed [MincerFS]. It's mandatory to make sure that mincer
 * class path is the same as compile class path of classes you wish to pass. In other case mincer won't be able to
 * generate proper stack maps or properly analyze classes hierarchy.
 *
 * Warning: Mincer can be used from multiple threads and considered thread-safe
 */
class Mincer private constructor(
    val fs: MincerFS,
    queues: List<MincerPipelineController<*>>
) {

    val debug = System.getProperty("mincer-debug") == "true"

    /**
     * Currently active pipelines
     */
    private val queues = queues.toMutableList()

    /**
     * It makes ASM nodes via bytecode in classpath
     */
    val nodes = NodesFactory(this::harvestBytecode)
    val factory = CachedFactory(BytecodeModelFactory(nodes))
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
            else if (b.isNotEmpty()) return b
            else empty = true

            Thread.sleep(1)
        }
        // If you ever see it just rerun build
        if (empty) throw java.lang.RuntimeException("Some multithreading cringe")
        val bytes = this.javaClass.classLoader.getResourceAsStream(name)?.readBytes()
            ?: ClassLoader.getSystemResourceAsStream(name)?.readBytes()
            ?: throw BytecodeModelFactory.BytecodeNotFoundException(name)

        cache[name] = bytes

        return bytes
    }

    /**
     * Sometimes mincer can predict classes that won't be filtered. So this method will return them.
     * It's better to use it prediction whenever possible
     *
     * @return classes that mincer won't skipp or `null` if we need to process all classes
     */
    fun targetClasses(): ArrayList<ClassTypeReference>? {
        val result = arrayListOf<ClassTypeReference>()

        for (queue in queues) {
            result += queue.targetClass() ?: return null
        }

        return result
    }

    /**
     * Processes given class and returns processing result
     *
     * Class have to be in mincer class path. It may seem inconvenient but
     * at least you have less chances to break contract that mincer class path is the
     * same as compile class path
     *
     * @param name name of class that mincer need to process
     */
    fun advance(name: ClassTypeReference): MincerResult {
        var acc = MincerResult(this, name, null, MincerResultType.SKIPPED)
        for (i in queues.indices) {
            acc = acc merge queues[i].process(this, acc.resultedNode, name)
            if (acc.type == MincerResultType.DELETED) return acc
        }

        return acc
    }

    /**
     * Advance all pipelines to next sub mincer.
     * If there's no next sub mincer pipeline is deleted from mincer
     *
     * If all pipelines are deleted there's no sense to continue using this mincer
     *
     * @return true if there are still some pipelines left
     */
    fun endIteration(): Boolean {
        queues.removeIf { !it.advance() }
        return queues.isNotEmpty()
    }

    /**
     * Method to convert [ClassNode] to [ByteArray] via ASM
     *
     * Mincer will use it's class path to generate stack maps here
     *
     * @param node node to convert
     * @return bytecode of the given node
     */
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

                for (name in supers1) {
                    if (supers2.any { it == name }) return name.name.toASMType().internalName
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