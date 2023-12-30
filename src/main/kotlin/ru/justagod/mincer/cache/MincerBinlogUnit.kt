package ru.justagod.mincer.cache

import ru.justagod.model.ClassTypeReference
import java.io.Serializable
import java.time.Instant

class MincerBinlogUnit<LogEvent: Any> : Serializable {

    val creationTime = Instant.now();
    val dependencies = arrayListOf<Dependency>()
    val events = arrayListOf<LogEvent>()


    fun declareDependency(name: ClassTypeReference) {
        dependencies += Dependency(name, Instant.now())
    }



    class Dependency(val name: ClassTypeReference, val time: Instant) : Serializable

}