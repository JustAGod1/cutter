package ru.justagod.plugin.gradle

import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.util.MincerDecentFS
import ru.justagod.cutter.mincer.util.MincerUtils
import ru.justagod.cutter.processing.CutterProcessingUnit
import ru.justagod.cutter.processing.config.CutterConfig
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DefaultCutterProcessor(private val threadsCount: Int, private val config: CutterConfig) : CutterProcessor {
    override fun process(root: File) {
        val pipeline = CutterProcessingUnit.makePipeline(config)

        val mincer = Mincer.Builder(MincerDecentFS(root))
            .registerPipeline(pipeline)
            .build()

        val printer = Executors.newSingleThreadExecutor()
        val listener = makeListener(printer)

        MincerUtils.processFolder(mincer, root, threadsCount, listener)

        printer.awaitTermination(5, TimeUnit.SECONDS)
        printer.shutdownNow()

        val result = pipeline.result()
        if (result.errors.isNotEmpty()) {
            System.err.println(CutterProcessingUnit.reportValidationResults(mapOf(config.targetSides to result.errors)))
            throw RuntimeException("Validation failed")
        }
    }

    private fun makeListener(printer: ExecutorService): MincerUtils.ProcessingListener {
        val listener = MincerUtils.ProcessingListener()
        var toProcess = 0
        var pass = 0
        val processed = AtomicInteger(0)
        var start = Instant.now()

        val stats = hashMapOf<MincerResultType, Int>()

        listener
            .onNewPass {
                pass++
                toProcess = it
                processed.set(0)
                val delta = Duration.between(start, Instant.now()).toMillis()
                start = Instant.now()
                stats.clear()
                if (pass > 1) printer.submit {
                    println()
                    println("Pass finished in $delta ms")
                }
            }
            .onProcessed { it, type ->
                val p = processed.incrementAndGet()

                val m = 20
                val progress =
                    (1..m).joinToString(separator = "") { if (p.toFloat() / toProcess >= it / m.toFloat()) "=" else " " }
                val msg = "\rPass: $pass; Progress: [$progress]($p/$toProcess): ${it.name}"
                printer.submit {
                    print(msg)
                    stats.merge(type, 1) { a, b -> a + b }
                }
            }

        return listener
    }
}