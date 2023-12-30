package ru.justagod.stackManipulation

/**
 * Ну начнем с того что это объективно ахуенная штука. Кто не согласен - тому в глаз.
 *
 * Идея очень простая. Каждый опкод жавы имеет максимум два действа: что-то делает и как-то меняет стек.
 * Может ничего не делать, может ничего не менять.
 *
 * Когда ебешься с асмом нужно постоянно следить за тем чтобы стек не нарушить. Т.е. не положить int там где
 * ожидался Set. В условиях асма это немного сложно время от времени и вот туть вот добрый вычер BytecodeAppender.
 *
 * Я завернул все нужные мне опкоды в инструкции которые лежат в файлике instructions.
 * Каждая из них имеет два метода: изменить стек, добавить инструкцию в визитор.
 *
 * Добавить инструкцию можно при помощи метода [append].
 * Сразу после вызова апендер вызовет изменение стека инструкцией и сразу же выкинет ошибку при нарушении типов.
 */
interface BytecodeAppender {

    fun append(instruction: BytecodeInstruction)

    fun append(vararg instructions: BytecodeInstruction) {
        for (instruction in instructions) {
            append(instruction)
        }
    }

    fun append(instructionSet: InstructionSet) {
        instructionSet.instructions.forEach(this::append)
    }

    operator fun plusAssign(instruction: BytecodeInstruction) {
        append(instruction)
    }

    operator fun plusAssign(instructionSet: InstructionSet) {
        append(instructionSet)
    }

    fun makeSetBuilder(): InstructionSet.Builder
}