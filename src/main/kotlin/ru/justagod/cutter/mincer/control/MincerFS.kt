package ru.justagod.cutter.mincer.control

import ru.justagod.cutter.mincer.Mincer
import ru.justagod.cutter.mincer.util.recursiveness.*
/**
 * Interface to implement complicated class paths of as described in [Mincer]
 *
 * Such as zip class path [MincerZipFS]
 * Or several class paths [MincerTreeFS]
 * And so on
 */
interface MincerFS {

    /**
     * Sometimes sub mincer can generate classes that should be written
     *
     * Note: may be used not only for classes but for generic files too. But it's anti-pattern
     *
     * @param path class file name
     * @param bytecode class file content
     */
    fun pushGeneratedClass(path: String, bytecode: ByteArray)

    /**
     * Reads class bytecode
     *
     * @param path file name. You don't need to append .class or smth
     * @return file content or null if file not found
     */
    fun pullClass(path: String): ByteArray?

}