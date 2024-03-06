package com.backend.softtrainer.interpreter

import com.backend.softtrainer.interpreter.engine.ConditionScriptEngine
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RunnerTest {
    lateinit var runner: Runner

    @BeforeEach
    fun setUp() {
        runner = Runner(
            ConditionScriptEngine()
        )
    }

    @Test
    fun runCode() {
        assert(
            runner.runPredicate("message whereId \"3\" and message.anyCorrect()")
        )
    }
}