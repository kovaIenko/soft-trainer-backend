package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.MessageRequestDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.MessageType;
import com.backend.softtrainer.entities.Role;
import com.backend.softtrainer.entities.flow.FlowQuestion;
import com.backend.softtrainer.entities.flow.MultipleChoiceQuestion;
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion;
import com.backend.softtrainer.entities.flow.Text;
import com.backend.softtrainer.entities.messages.EnterTextQuestionMessage;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.MultiChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.TextMessage;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.utils.Converter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MessageService {

  private ChatGptService chatGptService;

  private final ChatRepository chatRepository;
  private final MessageRepository messageRepository;

  /**
   * method accepts the message from the user and return answer from the chatgpt
   *
   * @param messageRequestDto
   * @return
   */
  public CompletableFuture<EnterTextQuestionMessage> getResponse(final MessageRequestDto messageRequestDto) {

    //store user's message
    var messageEntity = Converter.convert(messageRequestDto);
    messageRepository.save(messageEntity);

    var optionalChat = chatRepository.findByIdWithMessages(messageRequestDto.getChatId());

    Chat chat = optionalChat.orElseThrow(() -> new NoSuchElementException(String.format(
      "No chat with id %s",
      messageRequestDto.getChatId()
    )));

    //get response from chatgpt, store it and return to front
    return chatGptService.completeChat(Converter.convert(chat))
      .thenApply(messageDto -> EnterTextQuestionMessage.builder()
        .chatId(messageRequestDto.getChatId())
        .content(messageDto.content())
        .id(UUID.randomUUID().toString())
        .timestamp(LocalDateTime.now())
        .build()
      )
      .thenApply(messageRepository::save);
  }


  public List<Message> getAndStoreMessageByFlow(final List<FlowQuestion> flowQuestions, final String chatId) {
    List<Message> messages = flowQuestions.stream()
      .map(question -> convert(question, chatId))
      .collect(Collectors.toList());
    return messageRepository.saveAll(messages);
  }


  private Message convert(final FlowQuestion flowQuestion, final String chatId) {

    if (flowQuestion instanceof Text text) {
      return TextMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .messageType(MessageType.TEXT)
        //todo fix it
        .previousMessageId("")
        .role(Role.APP)
        .timestamp(LocalDateTime.now())
        .content(text.getText())
        .build();
    } else if (flowQuestion instanceof SingleChoiceQuestion singleChoiceQuestion) {
      return SingleChoiceQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .role(Role.APP)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        //todo fix it
        .previousMessageId("")
        .timestamp(LocalDateTime.now())
        .options(singleChoiceQuestion.getOptions())
        .correct(singleChoiceQuestion.getCorrect())
        .build();
    } else if (flowQuestion instanceof MultipleChoiceQuestion multipleChoiceQuestion) {
      return MultiChoiceQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .messageType(MessageType.MULTI_CHOICE_QUESTION)
        //todo fix it
        .previousMessageId("")
        .role(Role.APP)
        .timestamp(LocalDateTime.now())
        .options(multipleChoiceQuestion.getOptions())
        .correct(multipleChoiceQuestion.getCorrect())
        .build();

    }

    throw new RuntimeException("please add converting type of messages from the flow");
  }

}
