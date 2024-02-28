package com.backend.softtrainer.interpreter.entity

import com.backend.softtrainer.interpreter.engine.Engine

typealias FType0 = FunctionalType0
typealias FType1 = FunctionalType1<*>
typealias FType2 = FunctionalType2<*, *>
typealias FType3 = FunctionalType3<*, *, *>

interface FunctionalType {
    fun invokeWithEngine(engine: Engine, vararg any: Any): Any = with(engine) { invoke(*any) }
    fun Engine.invoke(vararg any: Any): Any
}

fun interface FunctionalType0 : FunctionalType {

    override fun Engine.invoke(vararg args: Any) = function()
    fun Engine.function(): Any
}

fun interface FunctionalType1<T> : FunctionalType {

    override fun Engine.invoke(vararg args: Any) = function(args[0] as T)
    fun Engine.function(argument: T): Any
}

fun interface FunctionalType2<T, R> : FunctionalType {

    override fun Engine.invoke(vararg args: Any) = function(args[0] as T, args[1] as R)
    fun Engine.function(argument0: T, argument1: R): Any
}

fun interface FunctionalType3<T, T1, T2> : FunctionalType {

    override fun Engine.invoke(vararg args: Any) = function(args[0] as T, args[1] as T1, args[2] as T2)
    fun Engine.function(argument0: T, argument1: T1, argument2: T2): Any
}