package com.backend.softtrainer.interpreter.astBuilder

import com.backend.softtrainer.interpreter.entity.Token

sealed interface AstNode {
    val token: Token
    val type get() = token.type

    fun asCommand() = this as? CommandNode
    fun asValue() = this as? ValueNode
}

class CommandNode(override val token: Token, val left: AstNode? = null, val right: AstNode? = null) : AstNode
class ValueNode(override val token: Token) : AstNode

fun buildAST(postfix: List<Token>): AstNode {
    val stack = mutableListOf<AstNode>()

    for (token in postfix) {
        if (token.type.argumentsSize == 0) {
            stack.add(ValueNode(token))
        } else {
            val right = stack.takeIf { it.isNotEmpty() && token.type.argumentsSize >= 1 }?.removeLast()
            val left = stack.takeIf { it.isNotEmpty() && token.type.argumentsSize >= 2 }?.removeLast()
            stack.add(CommandNode(token, left, right))
        }
    }

    return stack.removeLast()
}