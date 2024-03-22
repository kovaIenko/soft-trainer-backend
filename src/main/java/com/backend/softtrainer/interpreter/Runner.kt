package com.backend.softtrainer.interpreter

import com.backend.softtrainer.interpreter.astBuilder.buildAST
import com.backend.softtrainer.interpreter.astBuilder.shuntingYard
import com.backend.softtrainer.interpreter.astBuilder.tokenize
import com.backend.softtrainer.interpreter.engine.ConditionScriptEngine

class Runner(
    private val engine: ConditionScriptEngine = ConditionScriptEngine(),
) {

    fun reset() = engine.reset()

    fun loadLib(library: List<Pair<String, Any>>) = engine.loadLib(library)

    fun runPredicate(code: String): Boolean = runCode(code) as Boolean

    private fun runCode(code: String) = code
        .tokenize()
        .toList()
        .let(::shuntingYard)
        .let(::buildAST)
        .let(engine::execute)

}