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
import com.backend.softtrainer.entities.messages.*;
import com.backend.softtrainer.interpreter.Runner;
import com.backend.softtrainer.interpreter.entity.PredicateMessage;
import com.backend.softtrainer.interpreter.libs.MessageManagerLib;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.utils.Converter;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MessageService {

  private final ChatRepository chatRepository;
  private final MessageRepository messageRepository;
  private final FlowService flowService;
  private final Runner runner = new Runner();
  private ChatGptService chatGptService;

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
          .id(UUID.randomUUID().toString())
          .chatId(singleChoiceAnswerMessageDto.getChatId())
          .flowQuestion(flowQuestion)
          .timestamp(singleChoiceAnswerMessageDto.getTimestamp())
          .answer(singleChoiceAnswerMessageDto.getAnswer())
          .build();
        messageRepository.save(message);

        return figureOutNextMessages(messageRequestDto.getChatId(), flowQuestion.getOrderNumber());

      } else if (messageRequestDto instanceof MultiChoiceAnswerMessageDto multiChoiceAnswerMessageDto) {
        message = MultiChoiceAnswerMessage.builder()
          .messageType(MessageType.SINGLE_CHOICE_ANSWER)
          .role(Role.USER)
          .id(UUID.randomUUID().toString())
          .chatId(multiChoiceAnswerMessageDto.getChatId())
          .flowQuestion(flowQuestion)
          .timestamp(multiChoiceAnswerMessageDto.getTimestamp())
          .answer(multiChoiceAnswerMessageDto.getAnswer())
          .build();

        messageRepository.save(message);

        return figureOutNextMessages(messageRequestDto.getChatId(), flowQuestion.getOrderNumber());


      } else if (messageRequestDto instanceof EnterTextAnswerMessageDto enterTextAnswerMessageDto) {
        message = EnterTextAnswerMessage.builder()
          .messageType(MessageType.SINGLE_CHOICE_ANSWER)
          .role(Role.USER)
          .id(UUID.randomUUID().toString())
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
  private CompletableFuture<List<Message>> figureOutNextMessages(final Long chatId,
                                                                 final Long previousOrderNumber) {

    List<Message> messages = new ArrayList<>();
    var nextFlowQuestion = getNextFlowQuestion(chatId, previousOrderNumber);

    var nextMessage = convert(nextFlowQuestion, chatId);
    messages.add(nextMessage);
    messageRepository.save(nextMessage);

    while (!MessageType.getActionableMessageTypes().contains(nextFlowQuestion.getMessageType().name())) {

      nextFlowQuestion = getNextFlowQuestion(chatId, nextFlowQuestion.getOrderNumber());

      nextMessage = convert(nextFlowQuestion, chatId);
      messageRepository.save(nextMessage);
      messages.add(nextMessage);
    }

    return CompletableFuture.completedFuture(messages);
  }

  private FlowQuestion getNextFlowQuestion(
    final Long chatId,
    final Long previousOrderNumber
  ) {
    List<FlowQuestion> flowQuestions = flowService.findAllByPreviousOrderNumber(previousOrderNumber);

    if (flowQuestions.size() == 1) {
      return flowQuestions.get(0);
    }
    if (flowQuestions.isEmpty()) {
      //todo handle the case we don't find the next flow node
      throw new RuntimeException("Flow is ended!!!");
    }
    return findFirstByPredicate(chatId, flowQuestions);
  }

  private FlowQuestion findFirstByPredicate(
    final Long chatId,
    final List<FlowQuestion> flowQuestions
  ) {
    var messageManagerLib = new MessageManagerLib(
      (Long orderNumber) -> {
        Optional<Message> message = findUserMessageByOrderNumber(chatId, orderNumber);
        if (message.isPresent()) {
          return new PredicateMessage(message.get());
        } else {
          return null;
        }
      }
    );

    var nextFlowQuestions = flowQuestions
      .stream()
      .filter(
        flowQuestion -> {
          runner.reset();
          runner.loadLib(messageManagerLib.getLib());
          return runner.runPredicate(flowQuestion.getShowPredicate());
        }
      )
      .findFirst();

    return nextFlowQuestions.orElse(null);
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

  public List<Message> getAndStoreMessageByFlow(final List<FlowQuestion> flowQuestions, final Long chatId) {
    List<Message> messages = flowQuestions.stream()
      .map(question -> convert(question, chatId))
      .collect(Collectors.toList());
    return messageRepository.saveAll(messages);
  }

  private Message convert(final FlowQuestion flowQuestion, final Long chatId) {

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


  @NotNull
  public Optional<Message> findUserMessageByOrderNumber(final Long chatId, final long orderNumber) {
    return messageRepository.findAllUserMessagesByOrderNumber(chatId, orderNumber);
  }

}
