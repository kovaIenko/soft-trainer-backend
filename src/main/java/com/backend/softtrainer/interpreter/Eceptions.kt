class ValueNotExistException(valuePath: String) : Exception("Value path: $valuePath")
class FunctionNotExistException(valuePath: String) : Exception("Function path: $valuePath")

class IncorrectTypeException(mastType: String, givenType: String? = null) : Exception(
    "Mast type: $mastType" + givenType?.let { ", given type: $it" },
)

class NodeNotExistException : Exception()

class MessageNotFoundException : Exception()
class NotImplementedException(message: String) : Exception(message)