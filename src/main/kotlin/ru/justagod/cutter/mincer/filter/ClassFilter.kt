package ru.justagod.cutter.mincer.filter

import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.mincer.pipeline.MincerPipeline


/**
 * There is filter in each [MincerPipeline] segment. Filters may be used to not write
 * straight forward checks in the beginning of sub mincer processing such as hierarchy.
 *
 * But also it can be used to speed up processing. Mincer can predict needed classes via analyzing filters.
 * Here [Mincer.targetClasses]
 */
interface ClassFilter {

    /**
     * @return if given class should be processed
     */
    fun isValid(name: ClassTypeReference, mincer: Mincer): Boolean

}