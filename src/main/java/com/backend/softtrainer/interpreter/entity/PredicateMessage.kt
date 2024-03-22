package com.backend.softtrainer.interpreter.entity

import com.backend.softtrainer.entities.MessageType
import com.backend.softtrainer.entities.flow.MultipleChoiceQuestion
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion
import com.backend.softtrainer.entities.messages.Message
import com.backend.softtrainer.entities.messages.MultiChoiceAnswerMessage
import com.backend.softtrainer.entities.messages.SingleChoiceAnswerMessage
import com.backend.softtrainer.interpreter.Option

class PredicateMessage(val message: Message) {

    val id: String get() = message.id

    val previousMessageId: Long get() = message.flowQuestion.previousOrderNumber

    val viewPredicate: String? get() = message.flowQuestion.showPredicate

    val type: MessageType get() = message.messageType

    val stringAnswer: String?
        get() = message.run {
            when (this) {
                is MultiChoiceAnswerMessage -> answer
                is SingleChoiceAnswerMessage -> answer
                else -> null
            }
        }

    val options: List<Option>? = message.flowQuestion.run {
        when (this) {
            is MultipleChoiceQuestion -> correct to options
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