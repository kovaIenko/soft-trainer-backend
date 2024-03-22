package com.backend.softtrainer.interpreter.libs

import NotImplementedException
import com.backend.softtrainer.interpreter.engine.ValuePath
import com.backend.softtrainer.interpreter.entity.FunctionalType3
import com.backend.softtrainer.interpreter.entity.PredicateMessage
import com.backend.softtrainer.interpreter.entity.TokenType
import org.hibernate.internal.util.collections.CollectionHelper.listOf

fun interface MessageProvider {
    fun getMessages(): List<PredicateMessage>
}

class MessageManagerLib(
    messageProvider: (Long) -> PredicateMessage?,
) {

    val lib = listOf(
        TokenType.Where.expression!! to FunctionalType3 { variable: ValuePath, fieldName: String, value: Any ->
            val message = when (fieldName) {
                "id" -> messageProvider(value.toString().toLong())
                else -> throw NotImplementedException("Find by $fieldName not implemented")
            } ?: return@FunctionalType3 false

            saveValue(variable, message)
            true
        },
    )
}