package ru.justagod.cutter.processing.analization

import org.objectweb.asm.Opcodes
import ru.justagod.cutter.processing.config.CutterConfig
import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.processor.WorkerContext
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.processing.model.ClassAtom
import ru.justagod.cutter.processing.model.MethodAtom
import ru.justagod.cutter.processing.model.ProjectModel
import ru.justagod.cutter.utils.findMethod

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
                    val atom = MethodAtom(context.name, lambdaMethod)
                    model.atom(atom, invocator.sides)
                    model.rememberLambdaMethod(atom)
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