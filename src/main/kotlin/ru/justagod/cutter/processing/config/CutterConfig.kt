package ru.justagod.cutter.processing.config

import ru.justagod.cutter.model.ClassTypeReference
import java.io.Serializable

data class CutterConfig(
    val annotation: ClassTypeReference,
    val validationOverrideAnnotation: ClassTypeReference?,
    val primalSides: Set<SideName>,
    val targetSides: Set<SideName>,
    val invocators: List<InvokeClass>
) : Serializable