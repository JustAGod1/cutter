package ru.justagod.mincer.cache

import java.io.Serializable

class MincerCacheUnit<Event: Any>(val bytecode: ByteArray, val binlog: MincerBinlogUnit<Event>) : Serializable