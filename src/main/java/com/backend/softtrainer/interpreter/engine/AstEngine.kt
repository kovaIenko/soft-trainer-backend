package com.backend.softtrainer.interpreter.engine

import com.backend.softtrainer.interpreter.astBuilder.AstNode
import com.backend.softtrainer.interpreter.astBuilder.CommandNode
import com.backend.softtrainer.interpreter.astBuilder.ValueNode
import com.backend.softtrainer.interpreter.entity.TokenType

abstract class AstEngine(initValues: List<Pair<String, Any>>) : Engine(initValues) {

    fun execute(astNode: AstNode): Any = astNode.processing().returnValue()

    protected open fun AstNode?.processing(): ValuePath {
        require(this != null)
        val cachePath = getCachePath(this)

        if (hasCacheValue(this).not()) {
            val value = when (this) {
                is ValueNode -> processingValueNode()
                is CommandNode -> processingCommandNode()
            }

            if (value is ValuePath) {
                return value
            }

            saveValue(cachePath, value)
        }
        return cachePath
    }

    protected abstract fun CommandNode.processingCommandNode(): Any
    protected abstract fun ValueNode.processingValueNode(): Any

    fun hasCacheValue(node: AstNode) = _values.containsKey(getCachePath(node).path)

    fun getCachePath(node: AstNode) = ValuePath(node.toString())
    fun AstNode.newValuePath(newValuePath: String) = ValuePath(this.toString() + newValuePath)

    val TokenType.valuePath get() = ValuePath(this.expression.toString())
}