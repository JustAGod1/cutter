package ru.justagod.processing.cutter.config

import ru.justagod.cutter.data.DynSideMarker
import ru.justagod.model.ClassTypeReference
import java.io.Serializable

class CutterConfig(
    val annotation: ClassTypeReference,
    val validationOverrideAnnotation: ClassTypeReference?,
    val removeAnnotations: Boolean,
    val primalSides: Set<SideName>,
    val targetSides: Set<SideName>,
    val invocators: List<InvokeClass>
) : Serializable {



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CutterConfig

        if (annotation != other.annotation) return false
        if (validationOverrideAnnotation != other.validationOverrideAnnotation) return false
        if (removeAnnotations != other.removeAnnotations) return false
        if (primalSides != other.primalSides) return false
        if (targetSides != other.targetSides) return false
        if (invocators != other.invocators) return false

        return true
    }

    override fun hashCode(): Int {
        var result = annotation.hashCode()
        result = 31 * result + (validationOverrideAnnotation?.hashCode() ?: 0)
        result = 31 * result + removeAnnotations.hashCode()
        result = 31 * result + primalSides.hashCode()
        result = 31 * result + targetSides.hashCode()
        result = 31 * result + invocators.hashCode()
        return result
    }

    override fun toString(): String {
        return "CutterConfig(annotation=$annotation, validationOverrideAnnotation=$validationOverrideAnnotation, removeAnnotations=$removeAnnotations, primalSides=$primalSides, targetSides=$targetSides, invocators=$invocators)"
    }
}