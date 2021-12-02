package ru.justagod.cutter.processing.config

import ru.justagod.cutter.model.ClassTypeReference
import java.io.Serializable

/**
 * Describes invoke class.
 *
 * Invoke classes is bridge between sides. For example, you want to call server code from code that exists on both sides
 * ```kotlin
 * @GradleSideOnly(GradleSide.SERVER)
 * fun server() {}
 *
 * fun common() {
 *      id (Side.isServer()) server()
 * }
 * ```
 *
 * When you build client you'll get validation error saying you don't have method server(). But you sure your call occurs
 * only on server. So it's the case when you need invoke classes.
 *
 * Consider previous example with invoke class
 *
 * ```kotlin
 * @GradleSideOnly(GradleSide.SERVER)
 * fun server() {}
 *
 * fun common() {
 *      id (Side.isServer()) Invoke.server { server() }
 * }
 * ```
 *
 * No validation errors will be thrown and even body of the lambda will be cleared.
 *
 *
 * So to define invoke class you need to create java functional interface. Like
 * ```java
 * package kek;
 * public interface InvokeServer {
 *      void run();
 * }
 * ```
 *
 * Name of the invoke-class is `kek.InvokeServer`
 * Sides of the invoke-class is server in our case.
 * Functional method is `run` with desc `()V`
 *
 * When such an invoke-class is declared on each client build bodies of implementations will be cleared.
 * So code like
 * ```java
 * InvokeServer inv = () -> code();
 * ```
 * Will effectively turn into:
 * ```java
 * InvokeServer inv = () -> {};
 * ```
 * On every client build
 */
data class InvokeClass(
    /**
     * Fully qualified name of functional interface
     */
    val name: ClassTypeReference,
    /**
     * Sides on which bode of the invoke-class should not be erased
     */
    val sides: Set<SideName>,
    /**
     * Name and description of functional method
     */
    val functionalMethod: MethodDesc
) : Serializable {

    override fun toString(): String = "${name.name}.$functionalMethod $sides"

}