package com.backend.softtrainer.interpreter.engine

import NodeNotExistException
import com.backend.softtrainer.interpreter.astBuilder.CommandNode
import com.backend.softtrainer.interpreter.astBuilder.ValueNode
import com.backend.softtrainer.interpreter.entity.FType3
import com.backend.softtrainer.interpreter.entity.TokenType
import com.backend.softtrainer.interpreter.runnerValues.stdLibValues
import com.backend.softtrainer.interpreter.runnerValues.tokenFunctions
import utils.isPrimitive
import utils.toPrimitive

class ConditionScriptEngine(
    initValues: List<Pair<String, Any>> = listOf()
) : AstEngine(tokenFunctions + stdLibValues + initValues) {

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
        token.value.isPrimitive() -> token.value.toPrimitive()
        else -> ValuePath(token.value)
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