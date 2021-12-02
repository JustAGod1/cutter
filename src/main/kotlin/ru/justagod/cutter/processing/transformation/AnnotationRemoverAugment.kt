package ru.justagod.cutter.processing.transformation

import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.processor.WorkerContext
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.processing.base.MincerAugment
import ru.justagod.cutter.processing.transformation.validation.ValidationError
import ru.justagod.cutter.processing.transformation.validation.ValidationResult

class AnnotationRemoverAugment(private val annotation: ClassTypeReference) : MincerAugment<Unit, ValidationResult>(){
    override fun process(context: WorkerContext<Unit, ValidationResult>): MincerResultType {
        var modified = false
        modified = modified or (context.info.node().visibleAnnotations?.removeIf { it.desc == annotation.desc() } == true)
        modified = modified or (context.info.node().invisibleAnnotations?.removeIf { it.desc == annotation.desc() } == true)
        context.info.node().fields?.forEach {
            modified = modified or (it.visibleAnnotations?.removeIf { it.desc == annotation.desc() } == true)
            modified = modified or (it.invisibleAnnotations?.removeIf { it.desc == annotation.desc() } == true)
        }
        context.info.node().methods?.forEach {
            modified = modified or (it.visibleAnnotations?.removeIf { it.desc == annotation.desc() } == true)
            modified = modified or (it.invisibleAnnotations?.removeIf { it.desc == annotation.desc() } == true)
        }

        return if (modified) MincerResultType.MODIFIED else MincerResultType.SKIPPED
    }
}