package com.backend.softtrainer.interpreter.astBuilder

import com.backend.softtrainer.interpreter.entity.Token
import com.backend.softtrainer.interpreter.entity.TokenType
import java.util.*

val precedence = TokenType.entries.map { it to it.precedence }.filter { it.second > 0 }.toMap()

fun shuntingYard(input: List<Token>): List<Token> {
    val output = mutableListOf<Token>()
    val operators = Stack<Token>()

    for (token in input) {
        when {
            token.type.argumentsSize == 0 -> output.add(token)
            token.type is TokenType.OpeningBrackets -> operators.push(token)
            token.type is TokenType.ClosingBrackets -> {
                while (operators.isNotEmpty() && operators.peek().type is TokenType.OpeningBrackets) {
                    output.add(operators.pop())
                }
                operators.pop()
            }

            precedence.containsKey(token.type) -> {
                while (operators.isNotEmpty() && precedence.getOrElse(token.type) { 0 } <= precedence.getOrElse(
                        operators.peek().type
                    ) { 0 }) {
                    output.add(operators.pop())
                }
                operators.push(token)
            }
        }
    }

    while (operators.isNotEmpty()) {
        output.add(operators.pop())
    }

    return output.toList()
}