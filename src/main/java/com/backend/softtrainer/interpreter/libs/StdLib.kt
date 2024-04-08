package com.backend.softtrainer.interpreter.libs

import com.backend.softtrainer.interpreter.entity.FunctionalType1

val stdLib = listOf(
    ".*\\.not" to FunctionalType1 { arg0: Boolean -> arg0.not() },
)
