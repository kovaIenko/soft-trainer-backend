package com.backend.softtrainer.interpreter

import com.backend.softtrainer.interpreter.astBuilder.buildAST
import com.backend.softtrainer.interpreter.astBuilder.shuntingYard
import com.backend.softtrainer.interpreter.astBuilder.tokenize
import com.backend.softtrainer.interpreter.engine.ConditionScriptEngine
import com.backend.softtrainer.interpreter.engine.ValuePath

class Runner(
    private val engine: ConditionScriptEngine,
) {

    fun loadValue(valuePath: String, value: Any) {
        engine.saveValue(ValuePath(valuePath), value)
    }

    fun runCode(code: String) = code
        .tokenize()
        .toList()
        .let(::shuntingYard)
        .let(::buildAST)
        .let(engine::execute)
}