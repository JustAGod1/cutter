package ru.justagod.mincer.pipeline

import ru.justagod.mincer.filter.ClassFilter
import ru.justagod.mincer.filter.WalkThroughFilter
import ru.justagod.mincer.processor.SubMincer

/**
 * Фактически билдер для пайплайна обработки классов
 *
 * Допустим у вас есть задача сначала собрать все классы, которые наследуют от класса А, а потом записать их всех
 * в класс Б.
 * Тогда пайплайн будет что-то типа `ClassesCollector --(classes list)--> ClassesWriter`
 *
 * Описание его в коде
 * ```
 * Pipeline.make(ClassesCollector()).join(ClassesWriter()).unwind()
 * ```
 *
 * @param worker - минсер который будет обрабатывать классы на данном этапе пайплайна
 * @param filter - прежде чем попасть в минсер, будет спрошенно разрешение у фильтра
 * @param parent - предыдущий этап пайплайна
 *
 */
class MincerPipeline<in Input, Output> private constructor(
    val worker: SubMincer<in Input, Output>,
    internal val filter: ClassFilter,
    internal val parent: MincerPipeline<*, in Input>?,
    default: Output
) {

    /**
     * You must manually ensure thread safety while writing to the output!
     */
    var output: Output = default

    companion object {

        /**
         * @param worker - минсер который будет обрабатывать классы на данном этапе пайплайна
         * @param filter - прежде чем попасть в минсер, будет спрошенно разрешение у фильтра
         * @param default - дефолтный результат работы данного этапа пайплайна
         */
        fun <Output> make(
            worker: SubMincer<Unit, Output>,
            default: Output,
            filter: ClassFilter = WalkThroughFilter
        ): MincerPipeline<Unit, Output> {
            return MincerPipeline(
                worker,
                filter,
                null,
                default
            )
        }

        /**
         * @param worker - минсер который будет обрабатывать классы на данном этапе пайплайна
         * @param filter - прежде чем попасть в минсер, будет спрошенно разрешение у фильтра
         */
        fun make(
            worker: SubMincer<Unit, Unit>,
            filter: ClassFilter = WalkThroughFilter
        ): MincerPipeline<Unit, Unit> {
            return MincerPipeline(
                worker,
                filter,
                null,
                Unit
            )
        }

    }

    fun <Into>join(mincer: SubMincer<in Output, Into>, default: Into, filter: ClassFilter = WalkThroughFilter): MincerPipeline<Output, Into> {
        return MincerPipeline(
            mincer,
            filter,
            this,
            default
        )
    }

    fun join(mincer: SubMincer<in Output, Unit>, filter: ClassFilter = WalkThroughFilter): MincerPipeline<Output, Unit> {
        return MincerPipeline(
            mincer,
            filter,
            this,
            Unit
        )
    }

    fun build() = MincerPipelineController(unwind())

    private fun unwind(): MincerPipelineChain<Output> {
        val plainQueue = ArrayList<MincerPipeline<*, *>>()
        var pipeline: MincerPipeline<*, *>? = this
        while (pipeline != null) {
            plainQueue.add(0, pipeline)
            pipeline = pipeline.parent
        }

        val topPipeline = plainQueue[0]
        return if (plainQueue.size > 1) MincerPipelineChain(
            topPipeline,
            Unit,
            buildChain(plainQueue, 1)
        )
        else MincerPipelineChain(topPipeline, Unit, null)
    }

    private fun buildChain(plainQueue: List<MincerPipeline<*, *>>, index: Int): RawChainSegment {
        val next = if (index < plainQueue.lastIndex) buildChain(plainQueue, index + 1) else null
        return RawChainSegment(plainQueue[index], next)
    }
}