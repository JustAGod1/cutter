package ru.justagod.plugin.processing.model

import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.SideName

class InvokeClass(
        val name: ClassTypeReference,
        val sides: Set<SideName>,
        val functionalMethod: MethodDesc
) {

}