package com.backend.softtrainer.interpreter.entity

import ChoiceTaskMessage
import IncorrectTypeException
import Message
import MessageType
import Option
import SingleChoiceAnswer
import TextMessage
import User

class PredicateMessage(val message: Message) {
    // Message
    val id: String get() = message.messageInfo.id
    val previousMessageId: String get() = message.messageInfo.previousMessageId
    val owner: User get() = message.messageInfo.owner
    val viewPredicate: String? get() = message.messageInfo.viewPredicate
    val type: MessageType get() = message.type

    // TextMessage, ChoiceTaskMessage
    val text: String
        get() = null
            ?: letIfType<TextMessage, String> { text }
            ?: letIfType<ChoiceTaskMessage, String> { text }
            ?: throw IncorrectTypeException("TextMessage or ChoiceTaskMessage", message::class.simpleName)

    // ChoiceTaskMessage, SingleChoiceAnswer
    val options: List<Option>
        get() = null
            ?: letIfType<ChoiceTaskMessage, List<Option>> { options }
            ?: letIfType<SingleChoiceAnswer, List<Option>> { options }
            ?: throw IncorrectTypeException("ChoiceTaskMessage or SingleChoiceAnswer", message::class.simpleName)


    private inline fun <reified T, R> letIfType(block: T.() -> R): R? = (message as? T)?.block()
}