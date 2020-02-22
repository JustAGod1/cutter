package ru.justagod.plugin.processing.model

import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.SideName

class ProjectModel(val invokeClasses: List<InvokeClass>) {
    val sidesTree = SidesTree("root")

    val lambdaMethods = hashMapOf<ClassTypeReference, MutableSet<MethodDesc>>()
}