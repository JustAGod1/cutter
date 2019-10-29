package ru.justagod.mincer.control

class MincerResult(val resultedBytecode: ByteArray, val type: MincerResultType) {

    infix fun merge(other: MincerResult): MincerResult {
        if (other.type == MincerResultType.DELETED || other.type == MincerResultType.MODIFIED) return other
        return this
    }
}