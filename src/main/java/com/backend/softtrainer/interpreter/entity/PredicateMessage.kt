package com.backend.softtrainer.interpreter.entity

import com.backend.softtrainer.entities.MessageType
import com.backend.softtrainer.entities.flow.MultipleChoiceTask
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion
import com.backend.softtrainer.entities.messages.Message
import com.backend.softtrainer.entities.messages.MultiChoiceTaskAnswerMessage
import com.backend.softtrainer.entities.messages.SingleChoiceAnswerMessage
import com.backend.softtrainer.interpreter.Option

class PredicateMessage(val message: Message) {

    val id: String get() = message.id

    val previousMessageId: Long get() = message.flowNode.previousOrderNumber

    val viewPredicate: String? get() = message.flowNode.showPredicate

    val type: MessageType get() = message.messageType

    val stringAnswer: String?
        get() = message.run {
            when (this) {
                is MultiChoiceTaskAnswerMessage -> answer
                is SingleChoiceAnswerMessage -> answer
                else -> null
            }
        }

    val options: List<Option>? = message.flowNode.run {
        when (this) {
            is MultipleChoiceTask -> correct to options
            is SingleChoiceQuestion -> correct to options
            else -> null
        }
    }?.let { (correctString, optionsString) ->
        val answer = stringAnswer
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
