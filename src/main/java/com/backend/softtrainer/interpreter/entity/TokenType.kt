package com.backend.softtrainer.interpreter.entity

sealed class TokenType(
    val expression: String? = null,
    val regex: String? = null,
    val argumentsSize: Int = 2,
    val precedence: Int
) {
    object Equal : TokenType("==", precedence = 4)
    object More : TokenType(">", precedence = 4)
    object Less : TokenType("<", precedence = 4)

    object And : TokenType("and", precedence = 2)
    object Or : TokenType("or", precedence = 1)

    object Access : TokenType(".", "\\.", precedence = 5)
    object Invoke : TokenType("()", "\\(\\)", 1, precedence = 5)
    object Where : TokenType("where", "where(\\w*)", precedence = 4)

    object ConstString : TokenType(null, "\".*?\"", 0, precedence = 0)
    object ConstList : TokenType(null, "\\[.*?\\]", 0, precedence = 0)
    object Value : TokenType(null, "\\w+", 0, precedence = 0)

    object OpeningBrackets : TokenType("(", null, 0, precedence = 0)
    object ClosingBrackets : TokenType(")", null, 0, precedence = 0)
    ;

    companion object {
        val entries get() = listOf(Equal, More, Less, And, Or, Access, Invoke, Where, ConstString, ConstList, Value)

        fun entriesToRegex() = entries.joinToString(separator = "|") { it.regex ?: it.expression.orEmpty() }

        fun getTokenType(token: String) = entries
            .firstOrNull { it.expression == token || it?.regex?.toRegex()?.matches(token) ?: false }
            ?: throw Exception("Not supported \"$token\"")

    }
}