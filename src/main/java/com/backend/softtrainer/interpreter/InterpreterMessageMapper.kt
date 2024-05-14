package com.backend.softtrainer.interpreter

import com.backend.softtrainer.entities.enums.MessageType
import com.backend.softtrainer.entities.flow.MultipleChoiceTask
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion
import com.backend.softtrainer.entities.messages.MultiChoiceTaskAnswerMessage
import com.backend.softtrainer.entities.messages.SingleChoiceAnswerMessage
import com.oruel.conditionscript.Message
import com.oruel.conditionscript.Option
import com.backend.softtrainer.entities.messages.Message as DbMessage

class InterpreterMessageMapper {
    fun map(dbMessage: DbMessage) = Message(
        dbMessage.id,
        com.oruel.conditionscript.MessageType.entries
            .firstOrNull { it.name == dbMessage.messageType.name }
            ?: com.oruel.conditionscript.MessageType.Text,
        getOptions(dbMessage),
    )


    fun getType(message: DbMessage): MessageType = message.messageType

    fun getStringAnswer(message: DbMessage): String? = message.run {
        when (this) {
            is MultiChoiceTaskAnswerMessage -> answer
            is SingleChoiceAnswerMessage -> answer
            else -> null
        }
    }

    fun getOptions(backMessage: DbMessage): List<Option>? = backMessage.flowNode.run {
        when (this) {
            is MultipleChoiceTask -> correct to options
            is SingleChoiceQuestion -> correct to options
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