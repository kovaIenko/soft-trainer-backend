package com.backend.softtrainer.interpreter

import com.backend.softtrainer.entities.enums.MessageType
import com.backend.softtrainer.entities.flow.EnterTextQuestion
import com.backend.softtrainer.entities.flow.MultipleChoiceTask
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion
import com.backend.softtrainer.entities.messages.*
import com.oruel.conditionscript.Message
import com.oruel.conditionscript.Option
import org.slf4j.LoggerFactory
import com.backend.softtrainer.entities.messages.Message as DbMessage

class InterpreterMessageMapper {

    // Define your SLF4J logger in Kotlin:
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun map(dbMessage: DbMessage): Message {
        logger.info("InterpreterMessageMapper.map() called with dbMessage: $dbMessage")
        return Message(
            dbMessage.id,
            com.oruel.conditionscript.MessageType.entries
                .firstOrNull { it.name == dbMessage.messageType.name }
                ?: com.oruel.conditionscript.MessageType.Text,
            getOptions(dbMessage),
        )
    }

    fun getType(message: DbMessage): MessageType {
        logger.info("InterpreterMessageMapper.getType() called with message: $message")
        return message.messageType
    }

    fun getStringAnswer(message: DbMessage): String? {
        logger.info("getStringAnswer() called with message: $message")
        return message.run {
            when (this) {
                is MultiChoiceTaskQuestionMessage -> answer
                is SingleChoiceQuestionMessage -> answer
                is EnterTextQuestionMessage -> answer
                else -> null
            }
        }
    }

    fun getOptions(backMessage: DbMessage): List<Option>? {
        logger.info("InterpreterMessageMapper.getOptions() called with backMessage: $backMessage")
        return backMessage.flowNode.run {
            when (this) {
                is MultipleChoiceTask -> correct to options
                is SingleChoiceQuestion -> correct to options
                is EnterTextQuestion -> correct to options
                else -> null
            }
        }?.let { (correctString, optionsString) ->
            val answer = getStringAnswer(backMessage)
                ?.split("||")
                ?.map { it.trim() }

            val options = optionsString
                ?.split("||")
                ?.map { it.trim() }

            val correct = correctString
                .split("||")
                .map { it.trim().toInt() - 1 }

            options?.mapIndexed { index, option ->
                Option(
                    text = option,
                    isCorrected = correct.contains(index),
                    isSelected = answer?.contains(option) ?: false,
                )
            }
        }
    }
}
