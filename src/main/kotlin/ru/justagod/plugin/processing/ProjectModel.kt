package ru.justagod.plugin.processing

import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.SideName

class ProjectModel {
    val sidesTree = SidesTree("root")

    val invokeClasses = hashMapOf<ClassTypeReference, List<SideName>>()

}