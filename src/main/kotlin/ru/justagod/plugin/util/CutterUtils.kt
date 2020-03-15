package ru.justagod.plugin.util

import org.objectweb.asm.Opcodes
import ru.justagod.mincer.Mincer
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.processing.model.InvokeClass
import ru.justagod.plugin.processing.model.ProjectModel

object CutterUtils {

    /**
     * Iterates over invoke classes and tries to find one which is parent of [ref]
     *
     * If no other invoke classes are parents of [ref], first found invoke class will be returned
     * In other case exception will be thrown because it's ambiguous which invoke class cutter should use
     */
    fun findInvokeClass(ref: ClassTypeReference, mincer: Mincer, model: ProjectModel): InvokeClass? {
        var result: InvokeClass? = null

        for (invokeClass in model.invokeClasses) {
            if (mincer.inheritance.isChild(ref, invokeClass.name, considerInterfaces = true)) {
                if (result != null) error("$ref inherits more than one invoke class")
                result = invokeClass
            }
        }

        if (result != null && ref != result.name) {
            val refModel = mincer.factory.makeModel(ref, null)
            if (!refModel.interfaces.any { it.rawType == result.name } && refModel.superClass!!.rawType != result.name) {
                error("$ref implements ${result.name} but not like direct child. It's not allowed behavior.")
            }
        }

        return result
    }

    private val opcodes = hashMapOf<Int, String>()

    init {
        for (field in Opcodes::class.java.fields) {
            opcodes[field.get(null) as Int] = field.name
        }
    }

    fun opcodeToString(opcode: Int): String {
        return opcodes.getValue(opcode)
    }
}