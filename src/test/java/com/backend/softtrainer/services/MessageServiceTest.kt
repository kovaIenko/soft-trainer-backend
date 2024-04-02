package com.backend.softtrainer.services

import com.backend.softtrainer.dtos.messages.MessageRequestDto
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Test

class MessageServiceTest {

    @Test
    fun findFirstByPredicate() {
        val messageService = spyk(MessageService(mockk(), mockk(), mockk(), mockk()))

        val messageRequestDto = MessageRequestDto()
        messageRequestDto.chatId = 0

        val singleChoiceQuestion = SingleChoiceQuestion()
        singleChoiceQuestion.showPredicate = "message whereId \"3\" and message.anyCorrect()"

//        val singleMessage1 = MultiChoiceAnswerMessage()
//        singleMessage1.answer = "q1"
//        singleMessage1.flowNode = SingleChoiceQuestion()
//        (singleMessage1.flowNode as SingleChoiceQuestion).options = "q||q1||q2"
//        (singleMessage1.flowNode as SingleChoiceQuestion).correct = "2"


//        every { messageService.findUserMessageByOrderNumber(any(), any()) } returns Optional.of(singleMessage1)

//        assert(
//            messageService.findFirstByPredicate(0, listOf(singleChoiceQuestion)) == singleChoiceQuestion
//        )
    }
}
