package com.backend.softtrainer.interpreter.astBuilder

import com.backend.softtrainer.interpreter.entity.Token
import com.backend.softtrainer.interpreter.entity.TokenType

fun String.tokenize() = TokenType.entriesToRegex().toRegex()
    .findAll(this.lowercase())
    .map { it.value }
    .map { Token(it, TokenType.getTokenType(it)) }
