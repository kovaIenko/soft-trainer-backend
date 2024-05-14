package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.messages.EnterTextAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.dtos.messages.MultiChoiceTaskAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceTaskAnswerMessageDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.enums.MessageType;
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
import com.backend.softtrainer.exceptions.UserHyperParamException;
import com.backend.softtrainer.interpreter.InterpreterMessageMapper;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.utils.Converter;
import com.oruel.conditionscript.libs.MessageManagerLib;
import com.oruel.conditionscript.script.ConditionScriptRunnerKt;
import com.oruel.scriptforge.Runner;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

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
  private final UserHyperParameterService userHyperParameterService;
  private final Runner conditionScriptRunner = ConditionScriptRunnerKt.ConditionScriptRunner();
  private ChatGptService chatGptService;

  private final InterpreterMessageMapper interpreterMessageMapper = new InterpreterMessageMapper();

  private void verifyWhetherQuestionIsAlreadyAnswered(final List<Message> actionableMessages) throws
                                                                                              SendMessageConditionException {

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

    verifyWhetherQuestionIsAlreadyAnswered(actionableMessages);

    var messagesGroupedByFlowNode = actionableMessages.stream()
      .collect(Collectors.groupingBy(Message::getFlowNode));

    var messagesWithoutAnswer = messagesGroupedByFlowNode.values().stream().filter(collection -> collection.size() == 1)
      .findFirst();

    var question = messagesWithoutAnswer.get().get(0);

    verifyAnswerHasSameMessageTypeWithQuestion(question, messageRequestDto);

    Message message;

    var flowNode = question.getFlowNode();

    var chatOpt = chatRepository.findById(messageRequestDto.getChatId());

    if(chatOpt.isEmpty()) throw new NoSuchElementException(String.format("There is no such chat %s", messageRequestDto.getChatId()));

    var chat  = chatOpt.get();

    if (messageRequestDto instanceof SingleChoiceAnswerMessageDto singleChoiceAnswerMessageDto) {
      message = SingleChoiceAnswerMessage.builder()
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .role(ChatRole.USER)
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .flowNode(flowNode)
        //.timestamp(singleChoiceAnswerMessageDto.getTimestamp())
        .answer(singleChoiceAnswerMessageDto.getAnswer())
        .build();
      messageRepository.save(message);
      return figureOutNextMessages(chat, flowNode.getOrderNumber(), flowNode.getSimulation().getId());
    } else if (messageRequestDto instanceof SingleChoiceTaskAnswerMessageDto singleChoiceTaskAnswerMessageDto) {
      message = SingleChoiceTaskAnswerMessage.builder()
        .messageType(MessageType.SINGLE_CHOICE_TASK)
        .role(ChatRole.USER)
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .flowNode(flowNode)
        //.timestamp(singleChoiceTaskAnswerMessageDto.getTimestamp())
        .answer(singleChoiceTaskAnswerMessageDto.getAnswer())
        .correct(singleChoiceTaskAnswerMessageDto.getCorrect())
        .options(singleChoiceTaskAnswerMessageDto.getOptions())
        .build();
      messageRepository.save(message);

      return figureOutNextMessages(chat, flowNode.getOrderNumber(), flowNode.getSimulation().getId());

    } else if (messageRequestDto instanceof MultiChoiceTaskAnswerMessageDto multiChoiceAnswerMessageDto) {
      message = MultiChoiceTaskAnswerMessage.builder()
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .role(ChatRole.USER)
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .flowNode(flowNode)
        //.timestamp(multiChoiceAnswerMessageDto.getTimestamp())
        .answer(multiChoiceAnswerMessageDto.getAnswer())
        .options(multiChoiceAnswerMessageDto.getOptions())
        .correct(multiChoiceAnswerMessageDto.getAnswer())
        .build();

      messageRepository.save(message);
      return figureOutNextMessages(chat, flowNode.getOrderNumber(), flowNode.getSimulation().getId());

    } else if (messageRequestDto instanceof EnterTextAnswerMessageDto enterTextAnswerMessageDto) {
      message = EnterTextMessage.builder()
        .messageType(MessageType.ENTER_TEXT_QUESTION)
        .role(ChatRole.USER)
        .id(UUID.randomUUID().toString())
        .chat(chat)
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

  private void verifyAnswerHasSameMessageTypeWithQuestion(final Message question, final MessageRequestDto messageRequestDto) throws
                                                                                                                             SendMessageConditionException {
    if (!question.getMessageType().equals(messageRequestDto.getMessageType())) {
      throw new SendMessageConditionException(String.format(
        "Answer should have the same message_type with question but answer type: %s and question type: %s",
        messageRequestDto.getMessageType(),
        question.getMessageType()
      ));
    }
  }


  @NotNull
  private CompletableFuture<List<Message>> figureOutNextMessages(final Chat chat,
                                                                 final Long previousOrderNumber,
                                                                 final Long simulationId) throws SendMessageConditionException {
    List<Message> messages = new ArrayList<>();
    var nextFlowNodeOptional = getNextFlowNode(chat.getId(), previousOrderNumber, simulationId);

    if (nextFlowNodeOptional.isPresent()) {
      var nextFlowNode = nextFlowNodeOptional.get();
      var nextMessage = convert(nextFlowNode, chat);

      nextMessage = messageRepository.save(nextMessage);
      messages.add(nextMessage);


      while (!MessageType.getActionableMessageTypes().contains(nextFlowNode.getMessageType().name())) {
        nextFlowNodeOptional = getNextFlowNode(chat.getId(), nextFlowNode.getOrderNumber(), simulationId);
        if (nextFlowNodeOptional.isPresent()) {
          nextFlowNode = nextFlowNodeOptional.get();
          nextMessage = convert(nextFlowNode, chat);
          nextMessage = messageRepository.save(nextMessage);
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
    final Long simulationId) throws SendMessageConditionException {

    List<FlowNode> flowNodes = flowService.findAllByNameAndPreviousOrderNumber(simulationId, previousOrderNumber);

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
          return interpreterMessageMapper.map(messages.get());
        } else {
          return null;
        }
      },
      (String key) -> {
        try {
          return userHyperParameterService.getUserHyperParam(chatId, key).getValue();
        } catch (UserHyperParamException e) {
          throw new RuntimeException(e);
        }
      },
      (key, value) -> {
        try {
          userHyperParameterService.update(chatId, key, value);
        } catch (UserHyperParamException e) {
          throw new RuntimeException(e);
        }
        return true;
      }
    );

    var nextFlowNodes = flowNodes
      .stream()
      .filter(
        flowNode -> {
          conditionScriptRunner.resetLibs();
          conditionScriptRunner.loadLib(messageManagerLib.getLib());
          var predicate = flowNode.getShowPredicate();
          if (predicate == null || predicate.isEmpty()) {
            return true;
          } else {
            var res = conditionScriptRunner.runPredicate(predicate);
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

    var optionalChat = chatRepository.findByIdWithMessages(messageEntity.getChat().getId());

    Chat chat = optionalChat.orElseThrow(() -> new NoSuchElementException(String.format(
      "No chat with id %s",
      messageEntity.getChat().getId()
    )));

    //get response from chatgpt, store it and return to front
    return chatGptService.completeChat(Converter.convert(chat))
      .thenApply(messageDto -> {
                   var message =
                     EnterTextMessage.builder()
                       .chat(chat)
                       .content(messageDto.content())
                       .id(UUID.randomUUID().toString())
                       //.timestamp(LocalDateTime.now())
                       .build();
                   messageRepository.save(message);
                   return List.of(message);
                 }
      );
  }

  public List<Message> getAndStoreMessageByFlow(final List<FlowNode> flowNodes, final Chat chat) {
    List<Message> messages = flowNodes.stream()
//      .filter(Objects::nonNull)
      .map(question -> convert(question, chat))
      .collect(Collectors.toList());
    return messageRepository.saveAll(messages);
  }

  private Message convert(final FlowNode flowNode, final Chat chat) {

    if (flowNode instanceof Text text) {
      return TextMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .messageType(MessageType.TEXT)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .role(ChatRole.APP)
        // .timestamp(LocalDateTime.now())
        .content(text.getText())
        .build();
    } else if (flowNode instanceof SingleChoiceQuestion singleChoiceQuestion) {
      return SingleChoiceQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .role(ChatRole.APP)
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
        .chat(chat)
        .role(ChatRole.APP)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        //.timestamp(LocalDateTime.now())
        .content(enterTextQuestion.getPrompt())
        .build();
    } else if (flowNode instanceof SingleChoiceTask singleChoiceTask) {
      return SingleChoiceTaskQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .role(ChatRole.APP)
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
        .chat(chat)
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .flowNode(flowNode)
        //.character(flowNode.getCharacter())
        .role(ChatRole.APP)
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
