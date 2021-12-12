package ru.justagod.cutter.processing.config

import java.io.Serializable

/**
 * Just type-safe wrapper around string with one perk
 * equals is case-insensitive
 */
class SideName private constructor(val name: String) : Serializable {

    private val _parents = arrayListOf<SideName>()
    val parents: List<SideName>
        get() = _parents

    /**
     * Method for such rare cases when you want some sides to be down cast of others
     *
     * For example lets consider three sides: server, client, singleplayer
     *
     * So your use case: in singleplayer you have both server and client classes, but you don't have
     * singlepalayer classes in server build nor client.
     *
     * So you have class like
     *
     * ```
     * @GradleSideOnly(GradleSide.SERVER)
     * class A {
     *
     *      @GradleSideOnly(GradleSide.SINGLEPLAYER)
     *      fun integratedServer() {}
     *
     * }
     * ```
     *
     * Without extendsFrom `integratedServer` will always be deleted. But if declare singleplayer like
     * `val singleplayer = SideName.make("singleplayer").extendsFrom(server).extendsFrom(client)`
     *
     * Singleplayer will be like down-cast of server and client.
     * Singleplayer will be able to call server methods but server methods won't be able to call singleplayer methods
     * Same for client
     *
     */
    fun extendsFrom(side: SideName): SideName {
        _parents += side
        return this
    }

    companion object {
        private val cache = hashMapOf<String, SideName>()

        @JvmStatic
        fun make(name: String): SideName {
            return cache.computeIfAbsent(name.toLowerCase()) { SideName(name.toLowerCase()) }
        }
    }



    override fun toString(): String = "Side($name)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SideName

        if (!name.equals(other.name, ignoreCase = true)) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}