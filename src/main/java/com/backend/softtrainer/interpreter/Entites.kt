enum class MessageType {
    Text, SingleChoiceTask, MultiChoiceTask, SingleChoiceAnswer, Images, Videos, EnterText
}

data class User(val id: String, val name: String, val avatar: String)

data class MessageInfo(
    val id: String,
    val previousMessageId: String,
    val owner: User,
    val viewPredicate: String? = null,
)

data class Option(
    val id: String,
    val text: String,
    val isCorrected: Boolean,
    val isSelected: Boolean,
)

interface Message {
    val messageInfo: MessageInfo
    val type: MessageType
}

data class TextMessage(
    override val messageInfo: MessageInfo,
    val text: String,
) : Message {

    override val type = MessageType.Text
}

data class ChoiceTaskMessage(
    override val messageInfo: MessageInfo,
    val text: String,
    val options: List<Option>,
    val isMultiChoice: Boolean
) : Message {

    override val type = if (isMultiChoice) MessageType.MultiChoiceTask else MessageType.SingleChoiceTask
}

data class SingleChoiceAnswer(
    override val messageInfo: MessageInfo,
    val options: List<Option>,
) : Message {

    override val type = MessageType.SingleChoiceAnswer
}
