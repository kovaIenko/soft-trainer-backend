package com.backend.softtrainer.services

import com.backend.softtrainer.dtos.messages.MessageRequestDto
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion
import com.backend.softtrainer.entities.messages.MultiChoiceAnswerMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Test
import java.util.*

class MessageServiceTest {

    @Test
    fun findFirstByPredicate() {
        val messageService = spyk(MessageService(mockk(), mockk(), mockk(), mockk()))

        val messageRequestDto = MessageRequestDto()
        messageRequestDto.chatId = ""

        val singleChoiceQuestion = SingleChoiceQuestion()
        singleChoiceQuestion.showPredicate = "message whereId \"3\" and message.anyCorrect()"

        val singleMessage1 = MultiChoiceAnswerMessage()
        singleMessage1.answer = "q1"
        singleMessage1.flowQuestion = SingleChoiceQuestion()
        (singleMessage1.flowQuestion as SingleChoiceQuestion).options = "q||q1||q2"
        (singleMessage1.flowQuestion as SingleChoiceQuestion).correct = "2"


        every { messageService.findUserMessageByOrderNumber(any(), any()) } returns Optional.of(singleMessage1)

        assert(
            messageService.findFirstByPredicate(messageRequestDto, listOf(singleChoiceQuestion)) == singleChoiceQuestion
        )
    }
}