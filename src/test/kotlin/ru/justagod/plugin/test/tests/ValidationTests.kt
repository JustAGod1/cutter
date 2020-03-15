package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.Test
import ru.justagod.mincer.MincerBuilder
import ru.justagod.mincer.util.MincerDecentFS
import ru.justagod.mincer.util.MincerUtils
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.data.BakedCutterTaskData
import ru.justagod.plugin.data.SideName
import ru.justagod.plugin.processing.CutterPipelines
import ru.justagod.plugin.processing.model.InvokeClass
import ru.justagod.plugin.processing.model.MethodDesc
import ru.justagod.plugin.processing.pipeline.validation.data.ClassError
import ru.justagod.plugin.processing.pipeline.validation.data.FieldError
import ru.justagod.plugin.processing.pipeline.validation.data.MethodError
import ru.justagod.plugin.processing.pipeline.validation.data.ValidationError
import ru.justagod.plugin.test.base.TestingContext
import ru.justagod.plugin.test.base.context.StraightContext
import java.io.File

object ValidationTests {

    private val expectedErrors = setOf(
            MethodError(
                    methodHolder = ClassTypeReference("validation.A"),
                    methodName = "b",
                    methodDesc = "()V",
                    holder = ClassTypeReference("validation.A"),
                    name = "a",
                    src = "A.java",
                    line = 8
            ),
            FieldError(
                    fieldHolder = ClassTypeReference("validation.B"),
                    fieldName = "c",
                    holder = ClassTypeReference("validation.B"),
                    name = "a",
                    src = "B.java",
                    line = 11
            ),
            ClassError(
                    subject = ClassTypeReference("validation.C2"),
                    holder = ClassTypeReference("validation.C1"),
                    name = "a",
                    src = "C.java",
                    line = 0
            ),
            ClassError(
                    subject = ClassTypeReference("validation.C2"),
                    holder = ClassTypeReference("validation.C1"),
                    name = "b",
                    src = "C.java",
                    line = 0
            ),
            ClassError(
                    subject = ClassTypeReference("validation.C2"),
                    holder = ClassTypeReference("validation.C1"),
                    name = "c",
                    src = "C.java",
                    line = 0
            ),
            FieldError(
                    fieldHolder = ClassTypeReference("validation.D"),
                    fieldName = "a",
                    holder = ClassTypeReference("validation.D"),
                    name = "b",
                    src = "D.java",
                    line = 13
            )
    )

    @Test
    fun straight() {
        val compiled = StraightContext.compileSources(TestingContext.resolve("validation"))

        val server = SideName.make("SERVER")
        val client = SideName.make("CLIENT")
        val task = BakedCutterTaskData(
                name = "none",
                annotation = ClassTypeReference("ru.justagod.cutter.GradleSideOnly"),
                validationOverrideAnnotation = null,
                removeAnnotations = false,
                primalSides = setOf(server, client),
                targetSides = setOf(server),
                invocators = listOf(
                        InvokeClass(
                                ClassTypeReference("ru.justagod.cutter.invoke.InvokeServer"),
                                hashSetOf(SideName.make("SERVER")),
                                MethodDesc("run", "()V")
                        ),
                        InvokeClass(
                                ClassTypeReference("ru.justagod.cutter.invoke.InvokeClient"),
                                hashSetOf(SideName.make("CLIENT")),
                                MethodDesc("run", "()V")
                        )
                ),
                markers = emptyList()
        )
        val pipeline = CutterPipelines.makePipelineWithValidation(task)

        val mincer = MincerBuilder(MincerDecentFS(compiled), false)
                .registerSubMincer(pipeline)
                .build()
        MincerUtils.processFolder(mincer, compiled)


        val actualErrors = pipeline.value!!.getValue(client).toSet()
        if (actualErrors == expectedErrors) return

        val notRaisedErrors = hashSetOf<ValidationError>()
        for (expectedError in expectedErrors) {
            if (expectedError !in actualErrors) {
                notRaisedErrors += expectedError
            }
        }

        val falseErrors = hashSetOf<ValidationError>()
        for (actualError in actualErrors) {
            if (actualError !in expectedErrors) {
                falseErrors += actualError
            }
        }

        if (falseErrors.isNotEmpty()) {
            println("Errors that weren't supposed to exist:")
            for (falseError in falseErrors) {
                println(falseError)
            }
        }
        if (notRaisedErrors.isNotEmpty()) {
            if (falseErrors.isNotEmpty()) println()

            println("Errors that were supposed to exist:")
            for (notRaisedError in notRaisedErrors) {
                println(notRaisedError)
            }
        }

        assert(false)
    }

}