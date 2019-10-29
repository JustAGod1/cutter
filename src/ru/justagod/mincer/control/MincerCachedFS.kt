package ru.justagod.mincer.control

class MincerCachedFS(private val delegate: MincerFS) : MincerFS by delegate {

    private val cache = hashMapOf<String, MincerArchive?>()

    override fun pullArchive(id: String): MincerArchive? {
        return cache.computeIfAbsent(id) { delegate.pullArchive(id) }
    }

}