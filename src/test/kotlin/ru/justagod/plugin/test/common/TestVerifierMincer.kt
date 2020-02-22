package ru.justagod.plugin.test.common

import ru.justagod.mincer.processor.SubMincer
import ru.justagod.model.ClassTypeReference

abstract class TestVerifierMincer : SubMincer<Unit, Unit> {

    abstract fun mandatoryClasses(): Set<ClassTypeReference>
}