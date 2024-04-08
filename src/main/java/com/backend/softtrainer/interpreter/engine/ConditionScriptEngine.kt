package com.backend.softtrainer.interpreter.engine

import NodeNotExistException
import com.backend.softtrainer.interpreter.astBuilder.CommandNode
import com.backend.softtrainer.interpreter.astBuilder.ValueNode
import com.backend.softtrainer.interpreter.entity.FType3
import com.backend.softtrainer.interpreter.entity.TokenType
import com.backend.softtrainer.interpreter.libs.messageStdLib
import com.backend.softtrainer.interpreter.libs.stdLib
import com.backend.softtrainer.interpreter.libs.tokenLib
import com.backend.softtrainer.interpreter.utils.isPrimitive
import com.backend.softtrainer.interpreter.utils.toPrimitive

class ConditionScriptEngine(
    initValues: List<Pair<String, Any>> = listOf()
) : AstEngine(tokenLib + stdLib + messageStdLib + initValues) {

    override fun CommandNode.processingCommandNode(): Any = when (type) {
        TokenType.Access -> ValuePath(left?.processing()?.path + "." + right?.processing()?.path)

        TokenType.Invoke -> right?.asCommand()
            ?.let { executeFunction(it.processing(), it.left.processing()) }
            ?: throw NodeNotExistException()

        TokenType.Equal,
        TokenType.More,
        TokenType.Less,

        TokenType.And,
        TokenType.Or,
        -> executeFunction(type.valuePath, left.processing(), right.processing())

        TokenType.Where -> processingWhereOperator()

        else -> token.value
    }

    override fun ValueNode.processingValueNode(): Any = when {
        type == TokenType.ConstString -> token.value.trim('"')
        type == TokenType.ConstList -> token.value
            .trim('[', ']')
            .split(",")
            .map(::convertCodeToPrimitive)

        token.value.isPrimitive() -> token.value.toPrimitive()
        else -> ValuePath(token.value)
    }

    private fun convertCodeToPrimitive(code: String): Any = code.trim().let {
        when {
            TokenType.ConstString.regex?.toRegex()?.matches(it) == true -> it.trim('"')
            else -> it.toPrimitive()
        }
    }

    private fun CommandNode.processingWhereOperator() = run {
        val variable = left.processing()
        val fieldName = TokenType.Where.regex?.toRegex()?.find(token.value)?.groupValues?.get(1).orEmpty()

        findFunction<FType3>(type.valuePath)
            .invokeWithEngine(
                this@ConditionScriptEngine,

                variable,
                fieldName,
                right.processing().returnValue(),
            )
    }
}