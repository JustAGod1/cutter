package ru.justagod.processing.cutter.analization

import org.objectweb.asm.Opcodes
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.processing.cutter.model.ClassAtom
import ru.justagod.processing.cutter.model.MethodAtom
import ru.justagod.processing.cutter.model.MethodBodyAtom
import ru.justagod.processing.cutter.model.ProjectModel
import ru.justagod.utils.findMethod

class InheritanceMarkerAugment(private val model: ProjectModel, config: CutterConfig) : AnalysisAugment(config) {
    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        val me = ClassAtom(context.name)

        val superClass = ClassTypeReference.fromInternal(context.info.node().superName)
        model.join(me, ClassAtom(superClass))

        context.mincer.inheritance.walk(context.name) {
            if (it.name == context.name) return@walk
            val superNode = context.mincer.makeNode(it.name)

            if (superNode.access and Opcodes.ACC_INTERFACE != 0) {
                val invocator = config.invocators.find { i -> i.name == it.name } ?: return@walk

                val lambdaMethod = context.info.node().findMethod(invocator.functionalMethod.name, invocator.functionalMethod.desc)

                if (lambdaMethod != null) {
                    val atom = MethodBodyAtom(context.name, lambdaMethod)
                    model.atom(atom, invocator.sides)
                    model.join(atom, atom.parent())
                }

                return@walk
            }

            context.info.node().methods.forEach { method ->
                if (superNode.findMethod(method.name, method.desc) != null) {
                    model.join(MethodAtom(context.name, method), MethodAtom(it.name, method))
                }
            }




        }

        return MincerResultType.SKIPPED
    }

}