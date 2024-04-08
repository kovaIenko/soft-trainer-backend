package com.backend.softtrainer.interpreter

import com.backend.softtrainer.entities.flow.MultipleChoiceTask
import com.backend.softtrainer.entities.messages.MultiChoiceTaskAnswerMessage
import com.backend.softtrainer.interpreter.engine.ConditionScriptEngine
import com.backend.softtrainer.interpreter.entity.PredicateMessage
import com.backend.softtrainer.interpreter.libs.MessageManagerLib
import com.backend.softtrainer.interpreter.utils.printTreeDetails
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RunnerTest {
    lateinit var runner: Runner

    @BeforeEach
    fun setUp() {
        val multiChoiceAnswerMessage = MultiChoiceTaskAnswerMessage()
        val multipleChoiceTask = MultipleChoiceTask()
        multipleChoiceTask.correct = "1||2"
        multipleChoiceTask.options = "g||h||k"
        multiChoiceAnswerMessage.flowNode = multipleChoiceTask
        multiChoiceAnswerMessage.answer = "g||k"

        runner = Runner(
            ConditionScriptEngine(
                MessageManagerLib { PredicateMessage(multiChoiceAnswerMessage) }.lib
            )
        )
    }

    @Test
    fun compile() {
        val node = runner.compile("message whereId \"2\" and message.anyCorrect().not()")
        printTreeDetails(node)
    }

    @Test
    fun runCode() {
        assert(
            runner.runPredicate("message whereId \"3\" and message.anyCorrect()")
        )
        assert(
            runner.runPredicate("message whereId \"3\" and message.anyCorrect().not()").not()
        )
    }

    @Test
    fun runCodeWithBrackets() {
        val loadPredicate = "message whereId \"3\""
        val predicate = "message whereId \"3\" and message.anyCorrect()"
        val newPredicate = "message1 whereId \"3\" and message1.selected() == [3] or message1.selected() == [1 ,3]"

        val result = runner.runPredicate(newPredicate)
//        printTree(astTree)
        println(result)
        assert(result)
    }
}