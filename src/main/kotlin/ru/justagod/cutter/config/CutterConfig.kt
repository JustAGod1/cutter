package ru.justagod.processing.cutter.config

import ru.justagod.cutter.data.DynSideMarker
import ru.justagod.model.ClassTypeReference

class CutterConfig(
    val annotation: ClassTypeReference,
    val validationOverrideAnnotation: ClassTypeReference?,
    val removeAnnotations: Boolean,
    val primalSides: Set<SideName>,
    val targetSides: Set<SideName>,
    val invocators: List<InvokeClass>,
    val markers: List<DynSideMarker>,
)