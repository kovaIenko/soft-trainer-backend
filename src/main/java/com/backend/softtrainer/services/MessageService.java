package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.messages.EnterTextAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.dtos.messages.MultiChoiceTaskAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceTaskAnswerMessageDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.MessageType;
import com.backend.softtrainer.entities.Role;
import com.backend.softtrainer.entities.flow.EnterTextQuestion;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.flow.MultipleChoiceTask;
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion;
import com.backend.softtrainer.entities.flow.SingleChoiceTask;
import com.backend.softtrainer.entities.flow.Text;
import com.backend.softtrainer.entities.messages.EnterTextMessage;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskAnswerMessage;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceAnswerMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskAnswerMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.TextMessage;
import com.backend.softtrainer.exceptions.SendMessageConditionException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
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

  private void verifySendMessageConditions(final List<Message> actionableMessages) throws SendMessageConditionException {

    if (actionableMessages.isEmpty()) {
      throw new SendMessageConditionException("No messages should be answered");
    }
    //all questions are answered
    if (actionableMessages.size() % 2 == 0) {
      throw new SendMessageConditionException("All questions have been already answered");
    }
  }

  public CompletableFuture<List<Message>> buildResponse(final MessageRequestDto messageRequestDto) throws
                                                                                                   SendMessageConditionException {
    var actionableMessageTypes = MessageType.getActionableMessageTypes();
    var actionableMessages = messageRepository.getActionableMessage(actionableMessageTypes, messageRequestDto.getChatId());

    verifySendMessageConditions(actionableMessages);

    var messagesGroupedByFlowNode = actionableMessages.stream()
      .collect(Collectors.groupingBy(Message::getFlowNode));

    var messagesWithoutAnswer = messagesGroupedByFlowNode.values().stream().filter(collection -> collection.size() == 1)
      .findFirst();

    var question = messagesWithoutAnswer.get().get(0);

    Message message;

    var flowNode = question.getFlowNode();

    if (messageRequestDto instanceof SingleChoiceAnswerMessageDto singleChoiceAnswerMessageDto) {
      message = SingleChoiceAnswerMessage.builder()
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .role(Role.USER)
        .id(UUID.randomUUID().toString())
        .chatId(singleChoiceAnswerMessageDto.getChatId())
        .flowNode(flowNode)
        //.timestamp(singleChoiceAnswerMessageDto.getTimestamp())
        .answer(singleChoiceAnswerMessageDto.getAnswer())
        .build();
      messageRepository.save(message);
      return figureOutNextMessages(messageRequestDto.getChatId(), flowNode.getOrderNumber(), flowNode.getName());
    } else if (messageRequestDto instanceof SingleChoiceTaskAnswerMessageDto singleChoiceTaskAnswerMessageDto) {
      message = SingleChoiceTaskAnswerMessage.builder()
        .messageType(MessageType.SINGLE_CHOICE_TASK)
        .role(Role.USER)
        .id(UUID.randomUUID().toString())
        .chatId(singleChoiceTaskAnswerMessageDto.getChatId())
        .flowNode(flowNode)
        //.timestamp(singleChoiceTaskAnswerMessageDto.getTimestamp())
        .answer(singleChoiceTaskAnswerMessageDto.getAnswer())
        .correct(singleChoiceTaskAnswerMessageDto.getCorrect())
        .options(singleChoiceTaskAnswerMessageDto.getOptions())
        .build();
      messageRepository.save(message);

      return figureOutNextMessages(messageRequestDto.getChatId(), flowNode.getOrderNumber(), flowNode.getName());

    } else if (messageRequestDto instanceof MultiChoiceTaskAnswerMessageDto multiChoiceAnswerMessageDto) {
      message = MultiChoiceTaskAnswerMessage.builder()
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .role(Role.USER)
        .id(UUID.randomUUID().toString())
        .chatId(multiChoiceAnswerMessageDto.getChatId())
        .flowNode(flowNode)
        //.timestamp(multiChoiceAnswerMessageDto.getTimestamp())
        .answer(multiChoiceAnswerMessageDto.getAnswer())
        .options(multiChoiceAnswerMessageDto.getOptions())
        .correct(multiChoiceAnswerMessageDto.getAnswer())
        .build();

      messageRepository.save(message);
      return figureOutNextMessages(messageRequestDto.getChatId(), flowNode.getOrderNumber(), flowNode.getName());

    } else if (messageRequestDto instanceof EnterTextAnswerMessageDto enterTextAnswerMessageDto) {
      message = EnterTextMessage.builder()
        .messageType(MessageType.ENTER_TEXT_QUESTION)
        .role(Role.USER)
        .id(UUID.randomUUID().toString())
        .chatId(enterTextAnswerMessageDto.getChatId())
        //.timestamp(enterTextAnswerMessageDto.getTimestamp())
        .flowNode(flowNode)
        .content(enterTextAnswerMessageDto.getContent())
        .build();
      messageRepository.save(message);
      //chat gpt
      return chatGptResponse(message);
    } else {
      throw new SendMessageConditionException(
        "Send message has incorrect message type. It should be one of the actionable message type");
    }
  }


  @NotNull
  private CompletableFuture<List<Message>> figureOutNextMessages(final Long chatId,
                                                                 final Long previousOrderNumber,
                                                                 final String flowName) throws SendMessageConditionException {
    List<Message> messages = new ArrayList<>();
    var nextFlowNodeOptional = getNextFlowNode(chatId, previousOrderNumber, flowName);

    if (nextFlowNodeOptional.isPresent()) {
      var nextFlowNode = nextFlowNodeOptional.get();
      var nextMessage = convert(nextFlowNode, chatId);
      messages.add(nextMessage);
      messageRepository.save(nextMessage);

      while (!MessageType.getActionableMessageTypes().contains(nextFlowNode.getMessageType().name())) {
        nextFlowNodeOptional = getNextFlowNode(chatId, nextFlowNode.getOrderNumber(), flowName);
        if (nextFlowNodeOptional.isPresent()) {
          nextFlowNode = nextFlowNodeOptional.get();
          nextMessage = convert(nextFlowNode, chatId);
          messageRepository.save(nextMessage);
          messages.add(nextMessage);
        } else {
          break;
        }
      }
    }
    return CompletableFuture.completedFuture(messages);
  }

  private Optional<FlowNode> getNextFlowNode(
    final Long chatId,
    final Long previousOrderNumber,
    final String flowName) throws SendMessageConditionException {

    List<FlowNode> flowNodes = flowService.findAllByNameAndPreviousOrderNumber(flowName, previousOrderNumber);

    if (flowNodes.size() == 1) {
      return Optional.of(flowNodes.get(0));
    } else if (flowNodes.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(findFirstByPredicate(chatId, flowNodes));
  }

  private @NotNull
  FlowNode findFirstByPredicate(
    final Long chatId,
    final List<FlowNode> flowNodes
  ) throws SendMessageConditionException {
    var messageManagerLib = new MessageManagerLib(
      (Long orderNumber) -> {
        Optional<Message> messages = findQuestionUserMessageByOrderNumber(chatId, orderNumber);
        if (messages.isPresent()) {
          return new PredicateMessage(messages.get());
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
            var res = runner.runPredicate(predicate);
            var logString = String.format(
              "runPredicate: %s, flowNodeId: %s, result: %s",
              predicate,
              flowNode.getOrderNumber(),
              res
            );
            log.info("RunPredicate: " + logString);
            return res;
          }
        }
      )
      .findFirst()
      .orElseThrow(() -> {
        var errorMessage = String.format(
          "The interpretator cannot choose the next flow node from the list of %s with chatId: %s",
          flowNodes,
          chatId
        );
        log.error(errorMessage);
        return new SendMessageConditionException("Incorrect flow: there is a problem with flow. Not found next node.");
      });
    return nextFlowNodes;
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
                       //.timestamp(LocalDateTime.now())
                       .build();
                   messageRepository.save(message);
                   return List.of(message);
                 }
      );
  }

  public List<Message> getAndStoreMessageByFlow(final List<FlowNode> flowNodes, final Long chatId) {
    List<Message> messages = flowNodes.stream()
//      .filter(Objects::nonNull)
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
       // .timestamp(LocalDateTime.now())
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
       // .timestamp(LocalDateTime.now())
        .options(singleChoiceQuestion.getOptions())
        .correct(singleChoiceQuestion.getCorrect())
        .build();
    } else if (flowNode instanceof EnterTextQuestion enterTextQuestion) {
      return EnterTextMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .role(Role.APP)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        //.timestamp(LocalDateTime.now())
        .content(enterTextQuestion.getPrompt())
        .build();
    } else if (flowNode instanceof SingleChoiceTask singleChoiceTask) {
      return SingleChoiceTaskQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .role(Role.APP)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .flowNode(flowNode)
        //.character(flowNode.getCharacter())
        //.timestamp(LocalDateTime.now())
        .options(singleChoiceTask.getOptions())
        .correct(singleChoiceTask.getCorrect())
        .build();
    } else if (flowNode instanceof MultipleChoiceTask multipleChoiceQuestion) {
      return MultiChoiceTaskQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .flowNode(flowNode)
        //.character(flowNode.getCharacter())
        .role(Role.APP)
        //.timestamp(LocalDateTime.now())
        .options(multipleChoiceQuestion.getOptions())
        .correct(multipleChoiceQuestion.getCorrect())
        .build();
    }

    throw new RuntimeException("please add converting type of messages from the flow");
  }

  @NotNull
  public Optional<Message> findQuestionUserMessageByOrderNumber(final Long chatId, final long orderNumber) {
    return messageRepository.findQuestionUserMessagesByOrderNumber(chatId, orderNumber);
  }

}
