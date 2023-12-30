package ru.justagod.processing.cutter.analization

import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.processor.WorkerContext
import ru.justagod.model.ClassTypeReference
import ru.justagod.processing.cutter.model.ClassAtom
import ru.justagod.processing.cutter.model.MethodAtom
import ru.justagod.processing.cutter.model.MethodBodyAtom
import ru.justagod.processing.cutter.model.ProjectModel

class InnerClassesMarkerAugment(private val model: ProjectModel, config: CutterConfig) : AnalysisAugment(config) {
    override fun process(context: WorkerContext<Unit, Unit>): MincerResultType {
        val root = ClassAtom(context.name)

        val node = context.info.node()




        if (node.outerClass != null) {
            if (node.outerMethod != null) {
                model.join(
                    root, MethodBodyAtom(
                        ClassTypeReference.fromInternal(node.outerClass),
                        node.outerMethod,
                        node.outerMethodDesc
                    )
                )
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