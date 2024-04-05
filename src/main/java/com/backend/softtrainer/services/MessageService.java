package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.messages.*;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.MessageType;
import com.backend.softtrainer.entities.Role;
import com.backend.softtrainer.entities.flow.*;
import com.backend.softtrainer.entities.messages.*;
import com.backend.softtrainer.interpreter.Runner;
import com.backend.softtrainer.interpreter.entity.PredicateMessage;
import com.backend.softtrainer.interpreter.libs.MessageManagerLib;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.utils.Converter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
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

      var flowNode = originMessageOptional.get().getFlowNode();

      if (messageRequestDto instanceof SingleChoiceAnswerMessageDto singleChoiceAnswerMessageDto) {
        message = SingleChoiceAnswerMessage.builder()
          .messageType(MessageType.SINGLE_CHOICE_QUESTION)
          .role(Role.USER)
          .id(UUID.randomUUID().toString())
          .chatId(singleChoiceAnswerMessageDto.getChatId())
          .flowNode(flowNode)
          .timestamp(singleChoiceAnswerMessageDto.getTimestamp())
          .answer(singleChoiceAnswerMessageDto.getAnswer())
          .build();
        messageRepository.save(message);
        return figureOutNextMessages(messageRequestDto.getChatId(), flowNode.getOrderNumber());
      }
      else if (messageRequestDto instanceof SingleChoiceTaskAnswerMessageDto singleChoiceTaskAnswerMessageDto) {
        message = SingleChoiceTaskAnswerMessage.builder()
          .messageType(MessageType.SINGLE_CHOICE_TASK)
          .role(Role.USER)
          .id(UUID.randomUUID().toString())
          .chatId(singleChoiceTaskAnswerMessageDto.getChatId())
          .flowNode(flowNode)
          .timestamp(singleChoiceTaskAnswerMessageDto.getTimestamp())
          .answer(singleChoiceTaskAnswerMessageDto.getAnswer())
          .correct(singleChoiceTaskAnswerMessageDto.getCorrect())
          .options(singleChoiceTaskAnswerMessageDto.getOptions())
          .build();
        messageRepository.save(message);

        return figureOutNextMessages(messageRequestDto.getChatId(), flowNode.getOrderNumber());

      }
      else if (messageRequestDto instanceof MultiChoiceTaskAnswerMessageDto multiChoiceAnswerMessageDto) {
        message = MultiChoiceTaskAnswerMessage.builder()
          .messageType(MessageType.MULTI_CHOICE_TASK)
          .role(Role.USER)
          .id(UUID.randomUUID().toString())
          .chatId(multiChoiceAnswerMessageDto.getChatId())
          .flowNode(flowNode)
          .timestamp(multiChoiceAnswerMessageDto.getTimestamp())
          .answer(multiChoiceAnswerMessageDto.getAnswer())
          .options(multiChoiceAnswerMessageDto.getOptions())
          .correct(multiChoiceAnswerMessageDto.getAnswer())
          .build();

        messageRepository.save(message);

        return figureOutNextMessages(messageRequestDto.getChatId(), flowNode.getOrderNumber());
      }
      else if (messageRequestDto instanceof EnterTextAnswerMessageDto enterTextAnswerMessageDto) {
        message = EnterTextMessage.builder()
          .messageType(MessageType.ENTER_TEXT_QUESTION)
          .role(Role.USER)
          .id(UUID.randomUUID().toString())
          .chatId(enterTextAnswerMessageDto.getChatId())
          .timestamp(enterTextAnswerMessageDto.getTimestamp())
          .flowNode(flowNode)
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
    var nextFlowNode = getNextflowNode(chatId, previousOrderNumber);

    var nextMessage = convert(nextFlowNode, chatId);
    messages.add(nextMessage);
    messageRepository.save(nextMessage);

    while (!MessageType.getActionableMessageTypes().contains(nextFlowNode.getMessageType().name())) {

      nextFlowNode = getNextflowNode(chatId, nextFlowNode.getOrderNumber());

      nextMessage = convert(nextFlowNode, chatId);
      messageRepository.save(nextMessage);
      messages.add(nextMessage);
    }

    return CompletableFuture.completedFuture(messages);
  }

  private FlowNode getNextflowNode(
    final Long chatId,
    final Long previousOrderNumber
  ) {
    List<FlowNode> flowNodes = flowService.findAllByPreviousOrderNumber(previousOrderNumber);

    if (flowNodes.size() == 1) {
      return flowNodes.get(0);
    }
    if (flowNodes.isEmpty()) {
      //todo handle the case we don't find the next flow node
      throw new RuntimeException("Flow is ended!!!");
    }
    return findFirstByPredicate(chatId, flowNodes);
  }

  private FlowNode findFirstByPredicate(
    final Long chatId,
    final List<FlowNode> flowNodes
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

    var nextFlowNodes = flowNodes
      .stream()
      .filter(
        flowNode -> {
          runner.reset();
          runner.loadLib(messageManagerLib.getLib());

          var predicate = flowNode.getShowPredicate();
          if (predicate == null || predicate.isEmpty()) {
            return true;
          } else {
            log.info("runPredicate: " + predicate);
            return runner.runPredicate(predicate);
          }
        }
      )
      .findFirst();
    return nextFlowNodes.orElse(null);
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
                     EnterTextMessage.builder()
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

  public List<Message> getAndStoreMessageByFlow(final List<FlowNode> flowNodes, final Long chatId) {
    List<Message> messages = flowNodes.stream()
      .map(question -> convert(question, chatId))
      .collect(Collectors.toList());
    return messageRepository.saveAll(messages);
  }

  private Message convert(final FlowNode flowNode, final Long chatId) {

    if (flowNode instanceof Text text) {
      return TextMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .messageType(MessageType.TEXT)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .role(Role.APP)
        .timestamp(LocalDateTime.now())
        .content(text.getText())
        .build();
    } else if (flowNode instanceof SingleChoiceQuestion singleChoiceQuestion) {
      return SingleChoiceQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .role(Role.APP)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .flowNode(flowNode)
        //.character(flowNode.getCharacter())
        .timestamp(LocalDateTime.now())
        .options(singleChoiceQuestion.getOptions())
        .correct(singleChoiceQuestion.getCorrect())
        .build();
    }
    else if (flowNode instanceof EnterTextQuestion enterTextQuestion) {
      return EnterTextMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .role(Role.APP)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .timestamp(LocalDateTime.now())
        .content(enterTextQuestion.getPrompt())
        .build();
    }
    else if (flowNode instanceof SingleChoiceTask singleChoiceTask) {
      return SingleChoiceTaskQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .role(Role.APP)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .flowNode(flowNode)
        //.character(flowNode.getCharacter())
        .timestamp(LocalDateTime.now())
        .options(singleChoiceTask.getOptions())
        .correct(singleChoiceTask.getCorrect())
        .build();
    }
    else if (flowNode instanceof MultipleChoiceTask multipleChoiceQuestion) {
      return MultiChoiceTaskQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .flowNode(flowNode)
        //.character(flowNode.getCharacter())
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
