package com.backend.softtrainer.interpreter.utils

import com.backend.softtrainer.interpreter.astBuilder.AstNode
import com.backend.softtrainer.interpreter.astBuilder.CommandNode
import java.util.*

fun printTree(root: AstNode) {
    val queue: Queue<AstNode> = LinkedList()
    queue.add(root)

    while (queue.isNotEmpty()) {
        val size = queue.size
        repeat(size) {
            val node = queue.poll()
            print("${node.token.value} ")

            (node as? CommandNode)?.left?.let { queue.add(it) }
            (node as? CommandNode)?.right?.let { queue.add(it) }
        }
        println()
    }
}