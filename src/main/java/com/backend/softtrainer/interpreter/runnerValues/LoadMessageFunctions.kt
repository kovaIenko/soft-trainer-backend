package com.backend.softtrainer.interpreter.runnerValues

import MessageNotFoundException
import NotImplementedException
import com.backend.softtrainer.interpreter.engine.ValuePath
import com.backend.softtrainer.interpreter.entity.FunctionalType3
import com.backend.softtrainer.interpreter.entity.PredicateMessage
import com.backend.softtrainer.interpreter.entity.TokenType
import org.hibernate.internal.util.collections.CollectionHelper.listOf

fun interface MessageProvider {
    fun getMessages(): List<PredicateMessage>
}

class LoadMessageFunctions(
    provider: MessageProvider
) {

    val functions = listOf(
        TokenType.Where.expression!! to FunctionalType3 { variable: ValuePath, fieldName: String, value: Any ->
            val message = provider.getMessages().firstOrNull {
                when (fieldName) {
                    "id" -> it.id == value
                    else -> throw NotImplementedException("Find by $fieldName not implemented")
                }
            } ?: throw MessageNotFoundException()

            saveValue(variable, message)
            true
        },
    )

}