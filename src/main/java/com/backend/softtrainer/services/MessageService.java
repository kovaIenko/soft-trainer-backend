package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.messages.EnterTextAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.dtos.messages.MultiChoiceAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceAnswerMessageDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.MessageType;
import com.backend.softtrainer.entities.Role;
import com.backend.softtrainer.entities.flow.FlowQuestion;
import com.backend.softtrainer.entities.flow.MultipleChoiceQuestion;
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion;
import com.backend.softtrainer.entities.flow.Text;
import com.backend.softtrainer.entities.messages.EnterTextAnswerMessage;
import com.backend.softtrainer.entities.messages.EnterTextQuestionMessage;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.MultiChoiceAnswerMessage;
import com.backend.softtrainer.entities.messages.MultiChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceAnswerMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.TextMessage;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.utils.Converter;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MessageService {

  private ChatGptService chatGptService;

  private final ChatRepository chatRepository;
  private final MessageRepository messageRepository;
  private final FlowService flowService;

  public CompletableFuture<List<Message>> buildResponse(final MessageRequestDto messageRequestDto) {

    Optional<Message> originMessageOptional =
      messageRepository.getFirstByChatIdOrderByTimestampDesc(messageRequestDto.getChatId());

    if (originMessageOptional.isPresent()) {
      Message message;

      var flowQuestion = originMessageOptional.get().getFlowQuestion();

      if (messageRequestDto instanceof SingleChoiceAnswerMessageDto singleChoiceAnswerMessageDto) {
        message = SingleChoiceAnswerMessage.builder()
          .messageType(MessageType.SINGLE_CHOICE_ANSWER)
          .role(Role.USER)
          .chatId(singleChoiceAnswerMessageDto.getChatId())
          .flowQuestion(flowQuestion)
          .timestamp(singleChoiceAnswerMessageDto.getTimestamp())
          .answer(singleChoiceAnswerMessageDto.getAnswer())
          .build();

        messageRepository.save(message);

        return figureOutNextMessages(messageRequestDto, flowQuestion);

      } else if (messageRequestDto instanceof MultiChoiceAnswerMessageDto multiChoiceAnswerMessageDto) {
        message = MultiChoiceAnswerMessage.builder()
          .messageType(MessageType.SINGLE_CHOICE_ANSWER)
          .role(Role.USER)
          .chatId(multiChoiceAnswerMessageDto.getChatId())
          .flowQuestion(flowQuestion)
          .timestamp(multiChoiceAnswerMessageDto.getTimestamp())
          .answer(multiChoiceAnswerMessageDto.getAnswer())
          .build();

        messageRepository.save(message);

        return figureOutNextMessages(messageRequestDto, flowQuestion);

      } else if (messageRequestDto instanceof EnterTextAnswerMessageDto enterTextAnswerMessageDto) {
        message = EnterTextAnswerMessage.builder()
          .messageType(MessageType.SINGLE_CHOICE_ANSWER)
          .role(Role.USER)
          .chatId(enterTextAnswerMessageDto.getChatId())
          .timestamp(enterTextAnswerMessageDto.getTimestamp())
          .flowQuestion(flowQuestion)
          .content(enterTextAnswerMessageDto.getContent())
          .build();
        messageRepository.save(message);

        //chat gpt
        return chatGptResponse(message);
      }
    } else {
      throw new NoSuchElementException("No origin interactive origin message");
    }

    throw new NoSuchElementException("The incorrect interact type of message");
  }

  @NotNull
  private CompletableFuture<List<Message>> figureOutNextMessages(final MessageRequestDto messageRequestDto,
                                                                 final FlowQuestion flowQuestion) {

    var nextFlowQuestion = getNextFlowQuestion(flowQuestion);

    //todo find out all questions after nextFlowQuestiin
    var nextMessage = convert(nextFlowQuestion, messageRequestDto.getChatId());
    messageRepository.save(nextMessage);

    return CompletableFuture.completedFuture(List.of(nextMessage));
  }


  private FlowQuestion getNextFlowQuestion(final FlowQuestion previousFlowQuestion) {
    List<FlowQuestion> flowQuestions = flowService.findAllByPreviousOrderNumber(previousFlowQuestion.getOrderNumber());

    if (flowQuestions.isEmpty()) {
      //todo handle the case we don't find the next flow node
      throw new RuntimeException("Flow is ended!!!");
    }

//    ///todo Mih please take a look at this
//    var interpreter = new Runner(new ConditionScriptEngine());
//
//    return flowQuestions.stream()
//      .filter(question -> interpreter.runCode(question.getShowPredicate())).findFirst().orElse(flowQuestions.get(0));

    ///todo this as well
    return null;
  }

  private CompletableFuture<List<Message>> chatGptResponse(final Message messageEntity) {

    var optionalChat = chatRepository.findByIdWithMessages(messageEntity.getChatId());

    Chat chat = optionalChat.orElseThrow(() -> new NoSuchElementException(String.format(
      "No chat with id %s",
      messageEntity.getChatId()
    )));

    //get response from chatgpt, store it and return to front
    return chatGptService.completeChat(Converter.convert(chat))
      .thenApply(messageDto -> {
                   var message =
                     EnterTextQuestionMessage.builder()
                       .chatId(messageEntity.getChatId())
                       .content(messageDto.content())
                       .id(UUID.randomUUID().toString())
                       .timestamp(LocalDateTime.now())
                       .build();
                   messageRepository.save(message);
                   return List.of(message);
                 }
      );
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
        .flowQuestion(flowQuestion)
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
        .flowQuestion(flowQuestion)
        .timestamp(LocalDateTime.now())
        .options(singleChoiceQuestion.getOptions())
        .correct(singleChoiceQuestion.getCorrect())
        .build();
    } else if (flowQuestion instanceof MultipleChoiceQuestion multipleChoiceQuestion) {
      return MultiChoiceQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .messageType(MessageType.MULTI_CHOICE_QUESTION)
        .flowQuestion(flowQuestion)
        .role(Role.APP)
        .timestamp(LocalDateTime.now())
        .options(multipleChoiceQuestion.getOptions())
        .correct(multipleChoiceQuestion.getCorrect())
        .build();
    }

    throw new RuntimeException("please add converting type of messages from the flow");
  }

}
