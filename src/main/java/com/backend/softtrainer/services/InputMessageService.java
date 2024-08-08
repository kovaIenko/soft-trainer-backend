package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.MessageDto;
import com.backend.softtrainer.dtos.UserHyperParamResponseDto;
import com.backend.softtrainer.dtos.messages.EnterTextAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.HintMessageDto;
import com.backend.softtrainer.dtos.messages.LastSimulationMessageDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.dtos.messages.MultiChoiceTaskAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceTaskAnswerMessageDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Prompt;
import com.backend.softtrainer.entities.PromptName;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.flow.ContentQuestion;
import com.backend.softtrainer.entities.flow.EnterTextQuestion;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.flow.HintMessageNode;
import com.backend.softtrainer.entities.flow.MultipleChoiceTask;
import com.backend.softtrainer.entities.flow.ResultSimulationNode;
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion;
import com.backend.softtrainer.entities.flow.SingleChoiceTask;
import com.backend.softtrainer.entities.flow.Text;
import com.backend.softtrainer.entities.messages.ContentMessage;
import com.backend.softtrainer.entities.messages.EnterTextAnswerMessage;
import com.backend.softtrainer.entities.messages.EnterTextQuestionMessage;
import com.backend.softtrainer.entities.messages.HintMessage;
import com.backend.softtrainer.entities.messages.LastSimulationMessage;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskAnswerMessage;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceAnswerMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskAnswerMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.TextMessage;
import com.backend.softtrainer.exceptions.SendMessageConditionException;
import com.backend.softtrainer.interpreter.InterpreterMessageMapper;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.PromptRepository;
import com.backend.softtrainer.utils.Converter;
import com.oruel.conditionscript.libs.MessageManagerLib;
import com.oruel.conditionscript.script.ConditionScriptRunnerKt;
import com.oruel.scriptforge.Runner;
import jakarta.persistence.OptimisticLockException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class InputMessageService {

  private final ChatRepository chatRepository;
  private final FlowService flowService;
  private final UserHyperParameterService userHyperParameterService;
  private final Runner conditionScriptRunner = ConditionScriptRunnerKt.ConditionScriptRunner();

  private final ChatGptService chatGptService;
  private final PromptRepository promptRepository;

  private final UserDataExtractor userDataExtractor;

  private final MessageService messageService;

  private final InterpreterMessageMapper interpreterMessageMapper = new InterpreterMessageMapper();

  private final Map<String, String> HINT_CACHE = new ConcurrentHashMap<>();

  private final Map<String, String> RESULT_SIMULATION_CACHE = new ConcurrentHashMap<>();

  public CompletableFuture<List<Message>> buildResponse(final MessageRequestDto messageRequestDto) throws
                                                                                                   SendMessageConditionException {
    var chatOpt = chatRepository.findByIdWithMessages(messageRequestDto.getChatId());
    if (chatOpt.isEmpty()) {
      throw new NoSuchElementException(String.format("There is no such chat %s", messageRequestDto.getChatId()));
    }
    var chat = chatOpt.get();

    var allMessagesByChat = chat.getMessages().stream().sorted(Comparator.comparing(Message::getTimestamp)).toList();
    var messageOpt = allMessagesByChat.stream().filter(msg -> msg.getId().equals(messageRequestDto.getId())).findFirst();

    if (messageOpt.isEmpty()) {
      throw new NoSuchElementException(String.format(
        "There is no such message %s in the chat %s",
        messageRequestDto.getId(),
        messageRequestDto.getChatId()
      ));
    }

    var message = messageOpt.get();

    verifyUserAnswer(message, messageRequestDto);

    return findOutTheListOfMessagesBasedOnUserActionableMessage(messageRequestDto, chat, allMessagesByChat, message);
  }

  private @NotNull CompletableFuture<List<Message>> findOutTheListOfMessagesBasedOnUserActionableMessage(final MessageRequestDto messageRequestDto,
                                                                                                         final Chat chat,
                                                                                                         final List<Message> alreadyStoredMessages,
                                                                                                         Message currentMessage) throws
                                                                                                                                 SendMessageConditionException {
    Message message;

    var alreadyStoredMessagesAfterCurrent = getMessagesAfter(alreadyStoredMessages, currentMessage);
    if (messageRequestDto instanceof SingleChoiceAnswerMessageDto singleChoiceAnswerMessageDto) {
      message = SingleChoiceAnswerMessage.builder()
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .role(ChatRole.USER)
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .flowNode(currentMessage.getFlowNode())
        .interacted(true)
        .answer(singleChoiceAnswerMessageDto.getAnswer())
        .build();

      var answer = messageService.save(message);
      alreadyStoredMessagesAfterCurrent.add(answer);

      var nextMessages = figureOutNextMessagesWith(
        chat,
        currentMessage,
        alreadyStoredMessagesAfterCurrent
      );

      whetherItStartsGenerationHint(alreadyStoredMessagesAfterCurrent, currentMessage.getFlowNode(), chat);

      return nextMessages;
    } else if (messageRequestDto instanceof SingleChoiceTaskAnswerMessageDto singleChoiceTaskAnswerMessageDto) {
      message = SingleChoiceTaskAnswerMessage.builder()
        .messageType(MessageType.SINGLE_CHOICE_TASK)
        .role(ChatRole.USER)
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .flowNode(currentMessage.getFlowNode())
        .answer(singleChoiceTaskAnswerMessageDto.getAnswer())
        .correct(singleChoiceTaskAnswerMessageDto.getCorrect())
        .interacted(true)
        .options(singleChoiceTaskAnswerMessageDto.getOptions())
        .build();

      var answer = messageService.save(message);
      alreadyStoredMessagesAfterCurrent.add(answer);

      var nextMessages = figureOutNextMessagesWith(
        chat,
        currentMessage,
        alreadyStoredMessagesAfterCurrent
      );

      whetherItStartsGenerationHint(alreadyStoredMessagesAfterCurrent, currentMessage.getFlowNode(), chat);

      return nextMessages;
    } else if (messageRequestDto instanceof MultiChoiceTaskAnswerMessageDto multiChoiceAnswerMessageDto) {
      message = MultiChoiceTaskAnswerMessage.builder()
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .role(ChatRole.USER)
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .flowNode(currentMessage.getFlowNode())
        .answer(multiChoiceAnswerMessageDto.getAnswer())
        .options(multiChoiceAnswerMessageDto.getOptions())
        .interacted(true)
        .correct(multiChoiceAnswerMessageDto.getAnswer())
        .build();

      var answer = messageService.save(message);

      alreadyStoredMessagesAfterCurrent.add(answer);

      var nextMessages = figureOutNextMessagesWith(
        chat,
        currentMessage,
        alreadyStoredMessagesAfterCurrent
      );

      whetherItStartsGenerationHint(alreadyStoredMessagesAfterCurrent, currentMessage.getFlowNode(), chat);

      return nextMessages;
    } else if (messageRequestDto instanceof EnterTextAnswerMessageDto enterTextAnswerMessageDto) {
      message = EnterTextAnswerMessage.builder()
        .messageType(MessageType.ENTER_TEXT_QUESTION)
        .role(ChatRole.USER)
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .interacted(true)
        .flowNode(currentMessage.getFlowNode())
        .content(enterTextAnswerMessageDto.getAnswer())
        .build();

      var answer = messageService.save(message);
      alreadyStoredMessagesAfterCurrent.add(answer);

      var nextMessages = figureOutNextMessagesWith(
        chat,
        currentMessage,
        alreadyStoredMessagesAfterCurrent
      );

      whetherItStartsGenerationHint(alreadyStoredMessagesAfterCurrent, currentMessage.getFlowNode(), chat);

      return nextMessages;
    } else if (messageRequestDto instanceof LastSimulationMessageDto lastSimulationMessageDto) {

      waitForAiMsg(currentMessage);

      return CompletableFuture.completedFuture(List.of(currentMessage));
    } else if (messageRequestDto instanceof HintMessageDto hintMessageDto) {

      waitForAiMsg(currentMessage);

      return figureOutNextMessagesWith(
        chat,
        currentMessage,
        alreadyStoredMessagesAfterCurrent
      );
    } else {
      throw new SendMessageConditionException(
        "Send message has incorrect message type. It should be one of the actionable message type");
    }
  }

  public void waitForAiMsg(Message currentMessage) {
    final int maxRetries = 10;
    final long delay = 1000;

    for (int attempt = 0; attempt < maxRetries; attempt++) {
      try {
        if (Objects.nonNull(currentMessage)) {
          String content = getContent(currentMessage);

          if (currentMessage.getMessageType().equals(MessageType.HINT_MESSAGE)) {
            if (HINT_CACHE.containsKey(currentMessage.getId())) {
              var m = (HintMessage) currentMessage;
              m.setContent(HINT_CACHE.get(currentMessage.getId()));

              log.info("Message content of hint is already present in the cache: {}", m.getContent());
              HINT_CACHE.remove(currentMessage.getId());
              return;
            }
          } else if (currentMessage.getMessageType().equals(MessageType.RESULT_SIMULATION)) {
            if (RESULT_SIMULATION_CACHE.containsKey(currentMessage.getId())) {
              var m = (LastSimulationMessage) currentMessage;
              m.setContent(RESULT_SIMULATION_CACHE.get(currentMessage.getId()));

              log.info("Message content of result simulation is already present in the cache: {}", m.getContent());
              RESULT_SIMULATION_CACHE.remove(currentMessage.getId());
              return;
            }
          }

          if ((Objects.nonNull(content) && !content.isBlank())) {
            log.info("Message content is already present in the db: {}, version {}", content, currentMessage.getVersion());
            return;
          }

          log.info(
            "In time of request message {}, the content value {} is blank and version {}",
            currentMessage.getId(),
            content,
            currentMessage.getVersion()
          );

          Thread.sleep(delay);

          currentMessage = messageService.findMessageById(currentMessage.getId()).orElse(null);

          if (Objects.isNull(currentMessage)) {
            return;
          }

          log.info("After retry: {}, version {}", currentMessage, currentMessage.getVersion());
        }
      } catch (InterruptedException e) {
        log.error("Thread was interrupted", e);
        Thread.currentThread().interrupt();
        return;
      } catch (OptimisticLockException e) {
        log.info("Optimistic lock exception for message {}. Retrying... {}/{}", currentMessage.getId(), attempt + 1, maxRetries);
        try {
          Thread.sleep(delay);
        } catch (InterruptedException ie) {
          log.error("Thread was interrupted during retry sleep", ie);
          Thread.currentThread().interrupt();
          return;
        }
      } catch (Exception e) {
        log.error("Error while getting hint message while waiting ", e);
        return;
      }
    }
    log.error("Maximum retry attempts reached for message {}", currentMessage.getId());
  }

  private String getContent(Message message) {
    if (message instanceof LastSimulationMessage lastSimulationMessage) {
      return lastSimulationMessage.getContent();
    } else if (message instanceof HintMessage hintMessage) {
      return hintMessage.getContent();
    } else {
      return ((HintMessage) message).getContent();
    }
  }

  public static List<Message> getMessagesAfter(List<Message> messages, Message referenceMessage) {
    int index = messages.indexOf(referenceMessage);
    if (index == -1) {
      return new ArrayList<>();
    }
    return new ArrayList<>(messages.subList(index, messages.size()));
  }


  public void whetherItStartsGenerationHint(final List<Message> actionableMessages,
                                            final FlowNode actionableFlowNode,
                                            final Chat chat) {
    CompletableFuture.runAsync(() -> {
      try {
        var hintNodes = flowService.findAllBySimulationIdAndPreviousOrderNumber(
          actionableFlowNode.getSimulation().getId(),
          actionableFlowNode.getOrderNumber()
        );
        var hintNodeOpt = hintNodes.stream()
          .filter(node -> node.getMessageType().equals(MessageType.HINT_MESSAGE))
          .findFirst();

        if (hintNodeOpt.isPresent()) {
          log.info(
            "There is hint message after that node {} and simulation {}",
            actionableFlowNode.getOrderNumber(),
            actionableFlowNode.getSimulation().getId()
          );

          log.info(
            "Begin the generation of the content for hint message with order number {}",
            actionableFlowNode.getOrderNumber()
          );
          generateHint(actionableMessages, hintNodeOpt.get(), chat);
          log.info("The result of hint generation was updated in the db at {}", LocalDateTime.now());

        } else {
          log.info(
            "There is no hint message after that node {} and simulation {}",
            actionableFlowNode.getOrderNumber(),
            actionableFlowNode.getSimulation().getId()
          );
        }
      } catch (Exception e) {
        log.error("Error while generation of the hint message", e);
      }
    });

    log.info("We are completed with the generation of the hint message at {} for chat {}", LocalDateTime.now(), chat.getId());
  }

  @Deprecated
  private boolean isLastActionableMessageInteracted(final List<Message> messages) {
    if (messages.size() > 1) {
      var last = messages.get(messages.size() - 1);

      if (last.getMessageType().equals(MessageType.HINT_MESSAGE)) {
        return true;
      }
      var previousLast = messages.get(messages.size() - 2);
      return last.getMessageType().equals(previousLast.getMessageType());
      //last hint message with recommendation
    } else if (messages.size() == 1 && messages.get(0).getMessageType().equals(MessageType.HINT_MESSAGE)) {
      return true;
    } else {
      return messages.isEmpty();
    }
  }

  @NotNull
  private CompletableFuture<List<Message>> figureOutNextMessagesWith(final Chat chat,
                                                                     final Message currentMessage,
                                                                     final List<Message> alreadyStoredMessages) throws
                                                                                                                SendMessageConditionException {

    var flowNode = currentMessage.getFlowNode();
    boolean lastActionableMsgInteracted = isLastActionableMessageInteracted(alreadyStoredMessages);

    if (!lastActionableMsgInteracted) {
      return CompletableFuture.completedFuture(alreadyStoredMessages);
    }

    Long previousOrderNumber = flowNode.getOrderNumber();

    final Long simulationId = chat.getSimulation().getId();

    var nextFlowNodeOptional = getNextFlowNode(chat.getId(), previousOrderNumber, simulationId);

    if (nextFlowNodeOptional.isPresent()) {
      var nextFlowNode = nextFlowNodeOptional.get();
      var nextMessage = convert(nextFlowNode, chat);

      nextMessage = messageService.save(nextMessage);
      alreadyStoredMessages.add(nextMessage);

      while (!MessageType.getActionableMessageTypes().contains(nextFlowNode.getMessageType().name())) {
        nextFlowNodeOptional = getNextFlowNode(chat.getId(), nextFlowNode.getOrderNumber(), simulationId);
        if (nextFlowNodeOptional.isPresent()) {
          nextFlowNode = nextFlowNodeOptional.get();
          nextMessage = convert(nextFlowNode, chat);

          nextMessage = messageService.save(nextMessage);

          //todo temporary
          if (nextMessage.getMessageType().equals(MessageType.RESULT_SIMULATION)) {
            generateResultSimulationMessage(nextMessage, chat);
            log.info(
              "Begin the generation of the content for result message with order number {}",
              nextFlowNode.getOrderNumber()
            );
          }
          alreadyStoredMessages.add(nextMessage);
          System.out.println("The orderNumber of current flowNode is " + nextFlowNode.getOrderNumber());
        } else {
//          System.out.println("The orderNumber of current flowNode is " + nextFlowNode.getOrderNumber());

//          log.info("Start building last message for the last simulation node id: {}", nextFlowNode.getId());
//          var lastSimulationMessage = buildLastMessage(isOnboarding, chat);
//          lastSimulationMessage.ifPresent(alreadyStoredMessages::add);
//          lastSimulationMessage.ifPresent(msg -> log.info("Built message: {}", msg));
          chatRepository.updateIsFinished(chat.getId(), true);
          log.info("The chat with id {} is finished", chat.getId());

          var t = chatRepository.findById(chat.getId());
          log.info("The chat with id {} is finished", t.get().isFinished());
          break;
        }
      }
    }
    return CompletableFuture.completedFuture(alreadyStoredMessages);
  }

  public void generateHint(final List<Message> actionableMsgs, final FlowNode hintNode, final Chat chat) {
    log.info("Try to got necessary info for generating hint");
    try {

      Prompt simulationHintPrompt =
        promptRepository.findFirstByNameOrderByIdDesc(PromptName.SIMULATION_MESSAGE_HINT)
          .orElseThrow();

      log.info("The prompt for hint message is working {}", simulationHintPrompt.isOn());

      var mockContent = "Так тримати";

      if (simulationHintPrompt.isOn()) {
        var updatedChat = chatRepository.findByIdWithMessages(chat.getId()).orElseThrow();

        log.info("Current thread name is {}", Thread.currentThread().getName());
        generateAiHintAsync(updatedChat, actionableMsgs, null, simulationHintPrompt)
          .thenAccept(aiRecommendation -> {
            log.info("The hint we got from ai is {}", aiRecommendation.map(MessageDto::content));
            String content = aiRecommendation.map(MessageDto::content)
              .orElse(mockContent);

            messageService.updateOrCreateHintMessage(
              hintNode,
              chat,
              content,
              HINT_CACHE
            );
            log.info("We are done with updating the hint message at {}", LocalDateTime.now());
          }).get();
        log.info("The async future is completed {}", LocalDateTime.now());
      } else {
        messageService.updateOrCreateHintMessage(
          hintNode,
          chat,
          mockContent,
          HINT_CACHE
        );
      }

      log.info("prompt part is done {}", LocalDateTime.now());
      var msgs = messageService.findMessagesByOrderNumber(chat.getId(), hintNode.getOrderNumber());
      if (!msgs.isEmpty()) {
        log.info("The hint message is stored: {}, version {}", msgs.get(0), msgs.get(0).getVersion());
      } else {
        log.info("The is no hint message {}", hintNode.getOrderNumber());
      }

    } catch (Exception e) {
      log.error("Error while building hint message", e);
    }
  }

  private void generateResultSimulationMessage(final Message msg, final Chat chat) {
    CompletableFuture.runAsync(() -> {
      try {
        boolean isOnboarding = msg.getFlowNode().getSimulation().getName().equals("Onboarding");

        var title = isOnboarding ? "Вперед до змін!" : "Твій результат";
        var params = userHyperParameterService.findAllByChatId(chat.getId())
          .stream()
          .map(param -> new UserHyperParamResponseDto(param.getKey(), param.getValue()))
          .toList();

        if (isOnboarding) {
          messageService.updateResultSimulationMessage(
            msg,
            params,
            title,
            "Раді познайомитися. Го відточувати реальні навички комунікації!",
            RESULT_SIMULATION_CACHE
          );
          return;
        }

        var updatedChat = chatRepository.findByIdWithMessages(chat.getId()).orElseThrow();

        Prompt simulationRecommendationPrompt =
          promptRepository.findFirstByNameOrderByIdDesc(PromptName.SIMULATION_SUMMARY)
            .orElseThrow();

        if (simulationRecommendationPrompt.isOn()) {
          var aiRecommendation = generateAiSummary(updatedChat, params, simulationRecommendationPrompt);

          log.info("The simulation recommendation we got from ai is {}", aiRecommendation.map(MessageDto::content));
          String content = aiRecommendation.map(MessageDto::content)
            .orElse("Завжди є над чим працювати. Радий бачити, що ти продовжуєш тренувати свої soft-skills");

          messageService.updateResultSimulationMessage(
            msg,
            params,
            title,
            content,
            RESULT_SIMULATION_CACHE
          );

          log.info(
            "We are done with updating the result message at {}, and version {}",
            LocalDateTime.now(),
            RESULT_SIMULATION_CACHE.getOrDefault(msg.getId(), "not found")
          );
        } else {
          messageService.updateResultSimulationMessage(
            msg,
            params,
            title,
            "Завжди є над чим працювати. Радий бачити, що ти продовжуєш тренувати свої soft-skills",
            RESULT_SIMULATION_CACHE
          );
        }
      } catch (Exception e) {
        log.error("Error while building last message", e);
      }
    });
  }

  private Optional<FlowNode> getNextFlowNode(
    final Long chatId,
    final Long previousOrderNumber,
    final Long simulationId) throws SendMessageConditionException {

    List<FlowNode> flowNodes = flowService.findAllBySimulationIdAndPreviousOrderNumber(simulationId, previousOrderNumber);

    if (flowNodes.size() == 1) {
      return Optional.of(flowNodes.get(0));
    } else if (flowNodes.isEmpty()) {
      log.info("No flow nodes found for chatId: {} and previousOrderNumber: {}", chatId, previousOrderNumber);
      return Optional.empty();
    }
    return Optional.of(findFirstByPredicate(chatId, flowNodes));
  }

  private @NotNull FlowNode findFirstByPredicate(
    final Long chatId,
    final List<FlowNode> flowNodes
  ) throws SendMessageConditionException {

    log.info("Trying to find first by predicate in flowNodes {}", flowNodes);

    var messageManagerLib = new MessageManagerLib(
      (Long orderNumber) -> getMessage(chatId, orderNumber),
      (String key) -> userHyperParameterService.getOrCreateUserHyperParameter(chatId, key),
      (String key, Double value) -> userHyperParameterService.update(chatId, key, value)
    );
    log.info("Found nodes by order number: {}", flowNodes.stream().map(FlowNode::getOrderNumber).toList());

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

  @Nullable
  private com.oruel.conditionscript.Message getMessage(final Long chatId, final Long orderNumber) {
    log.info("Trying to find message by order number: {}", orderNumber);
    Optional<Message> messages = messageService.findQuestionUserMessageByOrderNumber(chatId, orderNumber);
    if (messages.isPresent()) {
      log.info("Message {} found by order number: {}", messages.get(), orderNumber);


      return interpreterMessageMapper.map(messages.get());
    } else {
      log.error("Message not found by order number: {}", orderNumber);
      //todo return null is not the best practice, using Optional is better
      return null;
    }
  }

  public Message convert(final FlowNode flowNode, final Chat chat) {

    if (flowNode instanceof Text text) {
      return TextMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .messageType(MessageType.TEXT)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .role(ChatRole.APP)
        .timestamp(LocalDateTime.now())
        .content(text.getText())
        .build();
    } else if (flowNode instanceof ContentQuestion contentQuestion) {
      return ContentMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .messageType(MessageType.CONTENT_QUESTION)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .role(ChatRole.APP)
        .timestamp(LocalDateTime.now())
        .content(contentQuestion.getUrl())
        .build();
    } else if (flowNode instanceof SingleChoiceQuestion singleChoiceQuestion) {
      return SingleChoiceQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .role(ChatRole.APP)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .timestamp(LocalDateTime.now())
        .options(singleChoiceQuestion.getOptions())
        .correct(singleChoiceQuestion.getCorrect())
        .build();
    } else if (flowNode instanceof EnterTextQuestion enterTextQuestion) {
      return EnterTextQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .role(ChatRole.APP)
        .messageType(MessageType.ENTER_TEXT_QUESTION)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .timestamp(LocalDateTime.now())
        .content(enterTextQuestion.getPrompt())
        .build();
    } else if (flowNode instanceof SingleChoiceTask singleChoiceTask) {
      return SingleChoiceTaskQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .role(ChatRole.APP)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .timestamp(LocalDateTime.now())
        .options(singleChoiceTask.getOptions())
        .correct(singleChoiceTask.getCorrect())
        .build();
    } else if (flowNode instanceof MultipleChoiceTask multipleChoiceQuestion) {
      return MultiChoiceTaskQuestionMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .role(ChatRole.APP)
        .timestamp(LocalDateTime.now())
        .options(multipleChoiceQuestion.getOptions())
        .correct(multipleChoiceQuestion.getCorrect())
        .build();
    } else if (flowNode instanceof HintMessageNode hintMessageNode) {
      return HintMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .messageType(MessageType.HINT_MESSAGE)
        .flowNode(flowNode)
        .title("Підказка")
        .character(flowNode.getCharacter())
        .role(ChatRole.APP)
        .timestamp(LocalDateTime.now())
        .build();
    } else if (flowNode instanceof ResultSimulationNode resultSimulationNode) {
      return LastSimulationMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .messageType(MessageType.RESULT_SIMULATION)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .role(ChatRole.APP)
        .timestamp(LocalDateTime.now())
        .build();
    }

    throw new RuntimeException("Please add converting type of messages from the flow");
  }

  public List<Message> getAndStoreMessageByFlow(final List<FlowNode> flowNodes, final Chat chat) {
    return flowNodes.stream()
      .filter(Objects::nonNull)
      .sorted(Comparator.comparing(FlowNode::getOrderNumber))
      .map(question -> convert(question, chat))
      .map(messageService::save)
      .sorted(Comparator.comparing(Message::getTimestamp))
      .toList();
  }

  private void verifyUserAnswer(final Message question, final MessageRequestDto messageRequestDto) throws
                                                                                                   SendMessageConditionException {
    if (!question.getMessageType().equals(messageRequestDto.getMessageType())) {
      throw new SendMessageConditionException(String.format(
        "Answer should have the same message_type with question but answer type: %s and question type: %s",
        messageRequestDto.getMessageType(),
        question.getMessageType()
      ));
    }
  }

  private CompletableFuture<Optional<MessageDto>> generateAiHintAsync(final Chat updatedChat,
                                                                      final List<Message> previousActionableMsg,
                                                                      final List<UserHyperParamResponseDto> params,
                                                                      final Prompt messageHintPrompt) {
    return CompletableFuture.supplyAsync(() -> userDataExtractor.getUserOnboardingData(updatedChat.getUser()))
      .thenCompose(onboardingStr -> {
        try {
          log.info("Extracted onboarding data: {}", onboardingStr);
          return chatGptService.buildAfterwardActionableHintMessage(
            Converter.convert(updatedChat),
            previousActionableMsg,
            messageHintPrompt,
            null,
            updatedChat.getSkill().getName(),
            onboardingStr
          ).thenApply(Optional::ofNullable);
        } catch (Exception e) {
          log.error("Error while generating AI hint", e);
          return CompletableFuture.completedFuture(Optional.empty());
        }
      });
  }

  private Optional<MessageDto> generateAiSummary(final Chat updatedChat,
                                                 final List<UserHyperParamResponseDto> params,
                                                 final Prompt simulationSummaryPrompt) {
    try {
      var onboardingExtraction = userDataExtractor.getUserOnboardingData(updatedChat.getUser());
      var summary = chatGptService.buildAfterwardSimulationRecommendation(
        Converter.convert(updatedChat),
        simulationSummaryPrompt,
        params.stream().collect(Collectors.toMap(
          UserHyperParamResponseDto::key,
          UserHyperParamResponseDto::value
        )),
        updatedChat.getSkill().getName(),
        onboardingExtraction
      ).get();
      log.error("everything is fine {} ", summary.content());
      //promptService.validateSimulationSummary(summary.content(), userName);
      return Optional.ofNullable(summary);

    } catch (Exception e) {
      log.error("Error while generating AI summary", e);
      return Optional.empty();
    }
  }

//  private void verifyWhetherQuestionIsAlreadyAnswered(final List<Message> actionableMessages) throws
//                                                                                              SendMessageConditionException {
//    if (actionableMessages.isEmpty()) {
//      throw new SendMessageConditionException("No messages should be answered");
//    }
//    if (actionableMessages.size() % 2 == 0) {
//      throw new SendMessageConditionException("All questions have been already answered");
//    }
//  }

//  private CompletableFuture<List<Message>> chatGptResponse(final Message messageEntity) {
//
//    var optionalChat = chatRepository.findByIdWithMessages(messageEntity.getChat().getId());
//
//    Chat chat = optionalChat.orElseThrow(() -> new NoSuchElementException(String.format(
//      "No chat with id %s",
//      messageEntity.getChat().getId()
//    )));
//
//    //get response from chatgpt, store it and return to front
//    return chatGptService.completeChat(Converter.convert(chat))
//      .thenApply(messageDto -> {
//                   var message =
//                     EnterTextAnswerMessage.builder()
//                       .chat(chat)
//                       .content(messageDto.content())
//                       .id(UUID.randomUUID().toString())
//                       //.timestamp(LocalDateTime.now())
//                       .build();
//                   messageService.save(message);
//                   return List.of(message);
//                 }
//      );
//  }

}
