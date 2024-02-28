package mock

import MessageInfo
import SingleChoiceAnswer
import User

val correctMessage = SingleChoiceAnswer(
    messageInfo = MessageInfo(
        id = "2",
        previousMessageId = "1",
        owner = User(
            id = "1",
            name = "user1",
            avatar = "",
        ),
    ),
    options = listOf(correctTrueOption, correctFalseOption)
)

val incorrectMessage = SingleChoiceAnswer(
    messageInfo = MessageInfo(
        id = "3",
        previousMessageId = "1",
        owner = User(
            id = "1",
            name = "user1",
            avatar = "",
        ),
    ),
    options = listOf(incorrectTrueOption, correctFalseOption)
)