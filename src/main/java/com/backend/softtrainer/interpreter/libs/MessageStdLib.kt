package com.backend.softtrainer.interpreter.libs

import com.backend.softtrainer.interpreter.entity.FunctionalType1
import com.backend.softtrainer.interpreter.entity.PredicateMessage

val messageStdLib = listOf(
    "message\\w*.allCorrect" to FunctionalType1 { message: PredicateMessage ->
        message.options?.all { it.isCorrected == it.isSelected } ?: false
    },

    "message\\w*.anyCorrect" to FunctionalType1 { message: PredicateMessage ->
        message.options?.any { it.isCorrected == it.isSelected } ?: false
    },

    "message\\w*.allIncorrect" to FunctionalType1 { message: PredicateMessage ->
        message.options?.all { it.isCorrected == it.isSelected }?.not() ?: false
    },

    "message\\w*.anyIncorrect" to FunctionalType1 { message: PredicateMessage ->
        message.options?.any { it.isCorrected == it.isSelected }?.not() ?: false
    },
)