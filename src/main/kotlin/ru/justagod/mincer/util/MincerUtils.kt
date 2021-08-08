package ru.justagod.mincer.util

import org.zeroturnaround.zip.ZipUtil
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.util.recursiveness.ByteArraySource
import ru.justagod.mincer.util.recursiveness.MincerFallbackFS
import ru.justagod.mincer.util.recursiveness.MincerTreeFS
import ru.justagod.mincer.util.recursiveness.MincerZipFS
import ru.justagod.model.ClassTypeReference
import java.io.File
import java.util.concurrent.*

object MincerUtils {

    fun processFolder(panel: Mincer, folder: File, threadsCount: Int = 10, listener: ProcessingListener = ProcessingListener()) {
        val executor = Executors.newFixedThreadPool(threadsCount)
        MincerUtils.processFolder(panel, executor, folder, threadsCount, listener)
        executor.shutdown()
    }
    fun processFolder(panel: Mincer, executor: ExecutorService, folder: File, threadsCount: Int = 10, listener: ProcessingListener = ProcessingListener()) {
        if (threadsCount < 1) error("threadsCount < 1")

        val completer = ExecutorCompletionService<Unit>(executor)
        do {
            val targetClasses = panel.targetClasses()
            val queue = if (targetClasses == null) getClassesInFolder(folder) else getFilesFromClasses(folder, targetClasses)

            listener.newPass(queue.size)
            val futures = arrayListOf<Future<*>>()
            try {
                repeat(threadsCount) {
                    futures += completer.submit {
                        var f = queue.poll()
                        while (f != null) {
                            if (Thread.interrupted()) return@submit

                            val name = f.absoluteFile.relativeTo(folder.absoluteFile)

                            val ref = ClassTypeReference.fromFilePath(name.path)
                            if (listener.filter(ref)) {
                                val result = panel.advance(ref).onModification {
                                    f.writeBytes(it)
                                }.onDeletion {
                                    f.delete()
                                }

                                listener.processed(ref, result.type)
                            } else {
                                listener.processed(ref, MincerResultType.SKIPPED)
                            }


                            f = queue.poll()
                        }
                    }
                }

                repeat(threadsCount) {
                    completer.take()
                }
                futures.forEach { it.get() }
            } catch (e: Exception) {
                futures.forEach { it.cancel(true) }
                throw e
            }
        } while (panel.endIteration())
        listener.newPass(0)
        folder.walkBottomUp().forEach { if (it.isDirectory) it.delete() }
    }

    fun getFilesFromClasses(root: File, classes: List<ClassTypeReference>): LinkedBlockingQueue<File> {
        val queue = LinkedBlockingQueue<File>()

        for (clazz in classes) {
            val file = root.resolve(clazz.internalName+".class")

            if (file.exists()) queue += file
        }

        return queue
    }

    fun getClassesInFolder(folder: File): LinkedBlockingQueue<File> {
        val queue = LinkedBlockingQueue<File>()
        folder.walkTopDown()
            .filter { it.path.endsWith(".class") && it.isFile }
            .forEach { queue += it }

        return queue
    }

    fun processFolderRecursively(root: File, factory: (MincerFS) -> Mincer) {
        val archives = arrayListOf<MincerZipFS>()
        for (s in root.listFiles()!!) {
            val extension = s.extension
            if (extension == "jar" || extension == "zip") {
                val entries = hashMapOf<String, ByteArraySource>()
                ZipUtil.iterate(s) { i, z ->
                    if (!z.isDirectory) {
                        entries[z.name] = ByteArraySource(z.name, i.readBytes())
                    }
                }
                archives += MincerZipFS(entries)
            }
        }

        val generalFS = MincerTreeFS(root, archives)


        archives.parallelStream().forEach { archive ->
            processArchive(generalFS, archive, factory)

        }
        processRoot(root, factory(generalFS))
    }

    fun processArchive(root: File, archiveFile: File, factory: (MincerFS) -> Mincer) {
        val entries = hashMapOf<String, ByteArraySource>()
        ZipUtil.iterate(archiveFile) { i, z ->
            if (!z.isDirectory) {
                entries[z.name] = ByteArraySource(z.name, i.readBytes())
            }
        }
        val archive = MincerZipFS(entries)
        val generalFS = MincerTreeFS(root, listOf(archive))
        processArchive(generalFS, archive, factory)
        archive.commit()
    }

    private fun processArchive(generalFS: MincerFS, archive: MincerZipFS, factory: (MincerFS) -> Mincer) {
        val mincer = factory(MincerFallbackFS(archive, generalFS))
        do {
            archive.entries.map { it.value }.parallelStream().forEach { entry ->
                if (!entry.path.endsWith(".class")) return@forEach

                val result = mincer.advance(ClassTypeReference.fromFilePath(entry.path))
                if (result.type == MincerResultType.MODIFIED) archive.entries[entry.path] = ByteArraySource(entry.path, result.bytecode())
                else if (result.type == MincerResultType.DELETED) archive.entries -= entry.path
            }
        } while (mincer.endIteration())
        archive.commit()
    }

    private fun processRoot(root: File, mincer: Mincer) {
        MincerUtils.processFolder(mincer, root)
    }

    fun readZip(file: File): HashMap<String, ByteArraySource> {
        val result = hashMapOf<String, ByteArraySource>()
        ZipUtil.iterate(file) { input, entry ->
            if (!entry.isDirectory)
                result[entry.name] = ByteArraySource(entry.name, input!!.readBytes(estimatedSize = 256))
        }

        return result

    }

    class ProcessingListener {

        private var newPass: ((toProcess: Int) -> Unit)? = null
        private var onProcessed: ((name: ClassTypeReference, result: MincerResultType) -> Unit)? = null
        private var filter: ((ClassTypeReference) -> Boolean)? = null


        fun filter(name: ClassTypeReference) = filter?.invoke(name) ?: true

        fun newPass(toProcess: Int) = newPass?.invoke(toProcess)

        fun processed(name: ClassTypeReference, result: MincerResultType) = onProcessed?.invoke(name, result)

        fun onFilter(block: (ClassTypeReference) -> Boolean): ProcessingListener {
            filter = block
            return this
        }

        fun onProcessed(block: (name: ClassTypeReference, result: MincerResultType) -> Unit): ProcessingListener {
            onProcessed = block
            return this
        }

        fun onNewPass(block: (toProcess: Int) -> Unit): ProcessingListener {
            newPass = block
            return this
        }
    }

}