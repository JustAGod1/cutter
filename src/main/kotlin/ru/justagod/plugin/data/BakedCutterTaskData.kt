package ru.justagod.plugin.data

import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.processing.model.InvokeClass

class BakedCutterTaskData(
        val name: String,
        val annotation: ClassTypeReference,
        val validationOverrideAnnotation: ClassTypeReference?,
        val removeAnnotations: Boolean,
        val primalSides: Set<SideName>,
        val targetSides: Set<SideName>,
        val invocators: List<InvokeClass>,
        val markers: List<DynSideMarker>,
        val cuttingMarkers: List<DynSideMarker> = markers
)