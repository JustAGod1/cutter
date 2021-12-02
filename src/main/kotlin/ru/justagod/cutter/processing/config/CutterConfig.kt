package ru.justagod.cutter.processing.config

import ru.justagod.cutter.model.ClassTypeReference
import java.io.Serializable

data class CutterConfig(
    /**
     * Name of annotation that used to mark sides
     */
    val annotation: ClassTypeReference,
    /**
     * Name of annotation that turns off validation
     */
    val validationOverrideAnnotation: ClassTypeReference?,
    /**
     * Fields/Methods/Classes that have no annotation will be marked with those sides
     */
    val primalSides: Set<SideName>,
    /**
     * As result of cutter execution only Fields/Methods/Classes that exists on those sides will survive
     */
    val targetSides: Set<SideName>,
    /**
     * Enumeration of invoke classes.
     * For explanation what invoke classes are consult [InvokeClass]
     */
    val invocators: List<InvokeClass>,
    /**
     * If true all annotation with name [annotation] will be deleted
     */
    val deleteAnnotations: Boolean
) : Serializable