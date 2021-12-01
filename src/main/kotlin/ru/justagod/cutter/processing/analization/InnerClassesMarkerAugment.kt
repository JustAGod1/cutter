package ru.justagod.cutter.processing.analization

import ru.justagod.cutter.processing.config.CutterConfig
import ru.justagod.cutter.mincer.control.MincerResultType
import ru.justagod.cutter.mincer.processor.WorkerContext
import ru.justagod.cutter.model.ClassTypeReference
import ru.justagod.cutter.processing.model.ClassAtom
import ru.justagod.cutter.processing.model.MethodAtom
import ru.justagod.cutter.processing.model.ProjectModel

class InnerClassesMarkerAugment(private val model: ProjectModel, config: CutterConfig) : AnalysisAugment(config) {
    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        val root = ClassAtom(context.name)

        val node = context.info.node()




        if (node.outerClass != null) {
            if (node.outerMethod != null) {
                model.join(root, MethodAtom(
                    ClassTypeReference.fromInternal(node.outerClass),
                    node.outerMethod,
                    node.outerMethodDesc
                ))
            } else {
                model.join(root, ClassAtom(ClassTypeReference.fromInternal(node.outerClass)))
            }
        } else {
            for (innerClass in node.innerClasses) {
                if (innerClass.name == context.name.internalName) {
                    model.join(
                        ClassAtom(context.name),
                        ClassAtom(ClassTypeReference.fromInternal(innerClass.outerName))
                    )
                }
            }
        }



        return MincerResultType.SKIPPED
    }


}