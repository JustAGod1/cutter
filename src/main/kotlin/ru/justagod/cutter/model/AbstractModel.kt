package ru.justagod.cutter.model

/**
 * All that code and children can be safely called legacy. You can completely ignore it.
 *
 * The only place where it is used is [InheritanceHelper]. But it uses like 1% of this code
 */
abstract class AbstractModel(val parent: AbstractModel?)
