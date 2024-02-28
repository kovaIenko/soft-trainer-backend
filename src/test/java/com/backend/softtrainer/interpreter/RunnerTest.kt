package com.backend.softtrainer.interpreter

import com.backend.softtrainer.interpreter.engine.ConditionScriptEngine
import com.backend.softtrainer.interpreter.entity.PredicateMessage
import com.backend.softtrainer.interpreter.runnerValues.LoadMessageFunctions
import com.backend.softtrainer.interpreter.runnerValues.MessageProvider
import mock.correctMessage
import mock.incorrectMessage
import org.hibernate.internal.util.collections.CollectionHelper.listOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RunnerTest {
    lateinit var runner: Runner

    @BeforeEach
    fun setUp() {
        runner = Runner(
            ConditionScriptEngine(
                LoadMessageFunctions(MessageProvider {
                    listOf(
                        PredicateMessage(incorrectMessage),
                        PredicateMessage(correctMessage)
                    )
                }).functions
            )
        )
    }

    @Test
    fun runCode() {
        assert(
            runner.runCode("message whereId \"3\" and message.anyCorrect()") == true
        )
    }
}