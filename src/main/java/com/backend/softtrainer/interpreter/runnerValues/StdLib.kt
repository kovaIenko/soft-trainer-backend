package com.backend.softtrainer.interpreter.runnerValues

import com.backend.softtrainer.interpreter.entity.FunctionalType1
import com.backend.softtrainer.interpreter.entity.PredicateMessage

val stdLibValues = listOf(
    "message\\w*.allCorrect" to FunctionalType1 { message: PredicateMessage -> message.options.all { it.isCorrected == it.isSelected } },
    "message\\w*.anyCorrect" to FunctionalType1 { message: PredicateMessage -> message.options.any { it.isCorrected == it.isSelected } },
)