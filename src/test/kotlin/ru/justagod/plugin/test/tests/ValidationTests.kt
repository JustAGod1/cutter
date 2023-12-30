package ru.justagod.plugin.test.tests

import org.junit.jupiter.api.Test
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.util.MincerDecentFS
import ru.justagod.mincer.util.MincerUtils
import ru.justagod.model.ClassTypeReference
import ru.justagod.plugin.test.base.TestingContext
import ru.justagod.plugin.test.base.context.StraightContext
import ru.justagod.processing.cutter.CutterProcessingUnit
import ru.justagod.processing.cutter.CutterProcessingUnit.client
import ru.justagod.processing.cutter.CutterProcessingUnit.server
import ru.justagod.processing.cutter.config.CutterConfig
import ru.justagod.processing.cutter.config.InvokeClass
import ru.justagod.processing.cutter.config.MethodDesc
import ru.justagod.processing.cutter.config.SideName
import ru.justagod.processing.cutter.transformation.validation.*

object ValidationTests {

    private val expectedErrors = setOf(
        MethodNotFoundValidationError(

            owner = ClassTypeReference("validation.A"),
            name = "b",
            desc = "()V",

            location = MethodBodyLocation(
                owner = ClassTypeReference("validation.A"),
                name = "a",
                source = "A.java",
                lineNumber = 8
            ),
            hisSides = setOf(server)
        ),
        FieldNotFoundValidationError(
            owner = ClassTypeReference("validation.B"),
            name = "c",
            location = MethodBodyLocation(
                owner = ClassTypeReference("validation.B"),
                name = "a",
                source = "B.java",
                lineNumber = 11
            ),
            hisSides = setOf(server)
        ),
        ClassNotFoundValidationError(
            clazz = ClassTypeReference("validation.C2"),
            location = FieldLocation(
                owner = ClassTypeReference("validation.C1"),
                name = "a",
                source = "C.java"
            ),
            hisSides = setOf(server)
        ),
        ClassNotFoundValidationError(
            clazz = ClassTypeReference("validation.C2"),
            location = MethodDescLocation(
                owner = ClassTypeReference("validation.C1"),
                name = "b",
                source = "C.java",
                lineNumber = 10
            ),
            hisSides = setOf(server)
        ),
        ClassNotFoundValidationError(
            clazz = ClassTypeReference("validation.C2"),
            location = MethodDescLocation(
                owner = ClassTypeReference("validation.C1"),
                name = "c",
                source = "C.java",
                lineNumber = 15
            ),
            hisSides = setOf(server)
        ),
        FieldNotFoundValidationError(
            owner = ClassTypeReference("validation.D"),
            name = "a",
            location = MethodBodyLocation(
                owner = ClassTypeReference("validation.D"),
                name = "b",
                source = "D.java",
                lineNumber = 13
            ),
            hisSides = setOf(server)
        ),
        FieldNotFoundValidationError(
            owner = ClassTypeReference("validation.D"),
            name = "a",
            location = MethodBodyLocation(
                owner = ClassTypeReference("validation.D"),
                name = "lambda\$b\$1",
                source = "D.java",
                lineNumber = 14
            ),
            hisSides = setOf(server)
        ),
        MethodNotFoundValidationError(
            owner = ClassTypeReference("validation.E"),
            name = "c",
            desc = "()V",
            location = MethodBodyLocation(
                owner = ClassTypeReference("validation.E"),
                name = "b",
                source = "E.java",
                lineNumber = 11
            ),
            hisSides = setOf(server)
        )
    )

    @Test
    fun straight() {
        val compiled = StraightContext.compileSources(TestingContext.resolve("validation"))

        val task = CutterConfig(
            annotation = ClassTypeReference("ru.justagod.cutter.GradleSideOnly"),
            validationOverrideAnnotation = null,
            primalSides = setOf(server, client),
            targetSides = setOf(client),
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
            removeAnnotations = false
        )
        val pipeline = CutterProcessingUnit.makePipeline(task)

        val mincer = Mincer.Builder(MincerDecentFS(compiled))
            .registerPipeline(pipeline)
            .build()
        MincerUtils.processFolder(mincer, compiled)


        val actualErrors = pipeline.result().errors.toSet()
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
                println(falseError.javaClass.simpleName + ": " + falseError)
            }
        }
        if (notRaisedErrors.isNotEmpty()) {
            if (falseErrors.isNotEmpty()) println()

            println("Errors that were supposed to exist:")
            for (notRaisedError in notRaisedErrors) {
                println(notRaisedError.javaClass.simpleName + ": " + notRaisedError)
            }
        }

        assert(false)
    }

}