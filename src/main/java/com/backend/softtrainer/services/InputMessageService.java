package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatDataDto;
import com.backend.softtrainer.dtos.ChatParams;
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
import com.backend.softtrainer.entities.messages.EnterTextQuestionMessage;
import com.backend.softtrainer.entities.messages.HintMessage;
import com.backend.softtrainer.entities.messages.LastSimulationMessage;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.TextMessage;
import com.backend.softtrainer.exceptions.SendMessageConditionException;
import com.backend.softtrainer.interpreter.InterpreterMessageMapper;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.MessageRepository;
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
  private final MessageRepository messageRepository;

  public CompletableFuture<ChatDataDto> buildResponse(final MessageRequestDto messageRequestDto) throws
                                                                                                 SendMessageConditionException {
    var chatOpt = chatRepository.findByIdWithMessages(messageRequestDto.getChatId());
    if (chatOpt.isEmpty()) {
      throw new NoSuchElementException(String.format("There is no such chat %s", messageRequestDto.getChatId()));
    }
    var chat = chatOpt.get();
    var allMessagesByChat = chat.getMessages().stream().sorted(Comparator.comparing(Message::getTimestamp)).toList();
    var messageOpt = allMessagesByChat.stream().filter(msg -> msg.getId().equals(messageRequestDto.getId())).findFirst();

    if (messageOpt.isEmpty()) {
      log.error(
        "There is no such message {} in the chat {}",
        messageRequestDto.getId(),
        messageRequestDto.getChatId()
      );

      return CompletableFuture.completedFuture(
        new ChatDataDto(allMessagesByChat, new ChatParams(chat.getHearts())));
    }

    var message = messageOpt.get();

    log.info("There is input message {} with message_type {} in the chat {}", message.getId(), message.getMessageType(), chat.getId());
    verifyUserAnswer(message, messageRequestDto);

    return findOutTheListOfMessagesBasedOnUserActionableMessage(messageRequestDto, chat, allMessagesByChat, message);
  }

  private @NotNull CompletableFuture<ChatDataDto> findOutTheListOfMessagesBasedOnUserActionableMessage(final MessageRequestDto messageRequestDto,
                                                                                                       final Chat chat,
                                                                                                       final List<Message> alreadyStoredMessages,
                                                                                                       Message currentMessage) throws
                                                                                                                               SendMessageConditionException {
    var alreadyStoredMessagesAfterCurrent = getMessagesAfter(alreadyStoredMessages, currentMessage);

    if (messageRequestDto instanceof SingleChoiceAnswerMessageDto singleChoiceAnswerMessageDto) {

      var currentMsg = (SingleChoiceQuestionMessage) currentMessage;
      currentMsg.setInteracted(true);
      currentMsg.setAnswer(singleChoiceAnswerMessageDto.getAnswer());
      currentMsg.setUserResponseTime(messageRequestDto.getUserResponseTime());
      currentMsg.setRole(ChatRole.USER);
      messageService.save(currentMsg);

      var nextMessages = figureOutNextMessagesWith(
        chat,
        currentMessage.getFlowNode(),
        alreadyStoredMessagesAfterCurrent
      );

      whetherItStartsGenerationHint(alreadyStoredMessagesAfterCurrent, currentMsg, chat);

      return nextMessages;
    } else if (messageRequestDto instanceof SingleChoiceTaskAnswerMessageDto singleChoiceTaskAnswerMessageDto) {

      var currentMsg = (SingleChoiceTaskQuestionMessage) currentMessage;
      currentMsg.setInteracted(true);
      currentMsg.setUserResponseTime(messageRequestDto.getUserResponseTime());
      currentMsg.setAnswer(singleChoiceTaskAnswerMessageDto.getAnswer());
      currentMsg.setRole(ChatRole.USER);
      messageService.save(currentMsg);

      var nextMessages = figureOutNextMessagesWith(
        chat,
        currentMessage.getFlowNode(),
        alreadyStoredMessagesAfterCurrent
      );

      whetherItStartsGenerationHint(alreadyStoredMessagesAfterCurrent, currentMessage, chat);

      return nextMessages;
    } else if (messageRequestDto instanceof MultiChoiceTaskAnswerMessageDto multiChoiceAnswerMessageDto) {

      var currentMsg = (MultiChoiceTaskQuestionMessage) currentMessage;
      currentMsg.setInteracted(true);
      currentMsg.setUserResponseTime(messageRequestDto.getUserResponseTime());
      currentMsg.setAnswer(multiChoiceAnswerMessageDto.getAnswer());
      currentMsg.setRole(ChatRole.USER);
      messageService.save(currentMsg);

      var nextMessages = figureOutNextMessagesWith(
        chat,
        currentMessage.getFlowNode(),
        alreadyStoredMessagesAfterCurrent
      );

      whetherItStartsGenerationHint(alreadyStoredMessagesAfterCurrent, currentMsg, chat);

      return nextMessages;
    } else if (messageRequestDto instanceof EnterTextAnswerMessageDto enterTextAnswerMessageDto) {
      var currentMsg = (EnterTextQuestionMessage) currentMessage;
      currentMsg.setUserResponseTime(messageRequestDto.getUserResponseTime());
      currentMsg.setInteracted(true);
      currentMsg.setAnswer(enterTextAnswerMessageDto.getAnswer());
      currentMsg.setContent(enterTextAnswerMessageDto.getAnswer());
      currentMsg.setRole(ChatRole.USER);

      messageService.save(currentMsg);

      var nextMessages = figureOutNextMessagesWith(
        chat,
        currentMessage.getFlowNode(),
        alreadyStoredMessagesAfterCurrent
      );

      whetherItStartsGenerationHint(alreadyStoredMessagesAfterCurrent, currentMsg, chat);

      return nextMessages;
    } else if (messageRequestDto instanceof LastSimulationMessageDto lastSimulationMessageDto) {

      var resultMessage = (LastSimulationMessage) currentMessage;

      var params = userHyperParameterService.findHyperParamsWithMaxValues(chat.getId())
        .stream()
        .map(param -> new UserHyperParamResponseDto(param.key(), param.value(), param.maxValue()))
        .toList();

      log.info("The params we got from user hyper parameters are {} and it size it {}", params, params.size());

      resultMessage.setHyperParams(params);

      if (Objects.isNull(resultMessage.getContent()) || resultMessage.getContent().isBlank()) {
        waitForAiMsg(resultMessage);
      }

      log.info("The result message looks like {}", resultMessage);
      return CompletableFuture.completedFuture(new ChatDataDto(List.of(resultMessage), new ChatParams(null)));
    } else if (messageRequestDto instanceof HintMessageDto hintMessageDto) {

      waitForAiMsg(currentMessage);
      log.info("The hint message looks like {}", currentMessage);
      return CompletableFuture.completedFuture(new ChatDataDto(List.of(currentMessage), new ChatParams(null)));
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
                                            final Message currentMessage,
                                            final Chat chat) {
    var actionableFlowNode = currentMessage.getFlowNode();

    Optional<FlowNode> nextHintNode = getFollowingHintNode(actionableFlowNode);
    if (actionableFlowNode.isHasHint() || nextHintNode.isPresent()) {

      currentMessage.setHasHint(true);

      if (nextHintNode.isEmpty()) {
        log.info(
          "There is not actual hint node when the actionable message has_hint = true and id {} and message_type {}",
          actionableFlowNode.getId(),
          actionableFlowNode.getMessageType()
        );
        return;
      }

      log.info(
        "There is hint message after that node {} and simulation {}",
        actionableFlowNode.getOrderNumber(),
        actionableFlowNode.getSimulation().getId()
      );

      var hintMessageId = UUID.randomUUID().toString();
      log.info("Generate uid for the hint message {}", hintMessageId);
      //todo temporary

      var hintMessage = HintMessage.builder()
        .id(hintMessageId)
        .chat(chat)
        .messageType(MessageType.HINT_MESSAGE)
        .flowNode(nextHintNode.get())
        .character(nextHintNode.get().getCharacter())
        .role(ChatRole.APP)
        .timestamp(LocalDateTime.now())
        .build();

      messageService.save(hintMessage);

      currentMessage.setHintMessage(hintMessage);

      CompletableFuture.runAsync(() -> {
        try {
          log.info(
            "Begin the generation of the content for hint message with order number {}",
            actionableFlowNode.getOrderNumber()
          );

          generateHintMessage(hintMessageId, actionableMessages, nextHintNode.get(), chat);
          log.info("The result of hint generation was updated in the db at {}", LocalDateTime.now());

        } catch (Exception e) {
          log.error("Error while generation of the hint message", e);
        }
      });

      log.info("We are completed with the generation of the hint message at {} for chat {}", LocalDateTime.now(), chat.getId());

    } else {
      log.info(
        "There is no hint message after that node {} and simulation {}",
        actionableFlowNode.getOrderNumber(),
        actionableFlowNode.getSimulation().getId()
      );
    }

  }

  private @NotNull Optional<FlowNode> getFollowingHintNode(final FlowNode actionableFlowNode) {
    var hintNodes = flowService.findAllBySimulationIdAndPreviousOrderNumber(
      actionableFlowNode.getSimulation().getId(),
      actionableFlowNode.getOrderNumber()
    );
    return hintNodes.stream()
      .filter(node -> node.getMessageType().equals(MessageType.HINT_MESSAGE))
      .findFirst();
  }

  @NotNull
  private CompletableFuture<ChatDataDto> figureOutNextMessagesWith(Chat chat,
                                                                   final FlowNode flowNode,
                                                                   final List<Message> alreadyStoredMessages) throws
                                                                                                              SendMessageConditionException {

    Long previousOrderNumber = flowNode.getOrderNumber();

    final Long simulationId = chat.getSimulation().getId();

    var nextFlowNodeOptional = getNextFlowNode(chat.getId(), previousOrderNumber, simulationId);

    if (nextFlowNodeOptional.isPresent()) {
      var nextFlowNode = nextFlowNodeOptional.get();
      var nextMessage = convert(nextFlowNode, chat);

      //todo remove it
      if (!nextFlowNode.getMessageType().equals(MessageType.HINT_MESSAGE)) {
        nextMessage = messageService.save(nextMessage);
        alreadyStoredMessages.add(nextMessage);
      }

      while (!MessageType.getActionableMessageTypes().contains(nextFlowNode.getMessageType().name())) {
        nextFlowNodeOptional = getNextFlowNode(chat.getId(), nextFlowNode.getOrderNumber(), simulationId);
        if (nextFlowNodeOptional.isPresent()) {
          nextFlowNode = nextFlowNodeOptional.get();

          nextMessage = convert(nextFlowNode, chat);

          //todo remove it
          if (!nextFlowNode.getMessageType().equals(MessageType.HINT_MESSAGE)) {
            nextMessage = messageService.save(nextMessage);
          }

          //todo temporary
          if (nextMessage.getMessageType().equals(MessageType.RESULT_SIMULATION)) {
            generateResultSimulationMessage(nextMessage, chat);
            log.info(
              "Begin the generation of the content for result message with order number {}",
              nextFlowNode.getOrderNumber()
            );
          }
          alreadyStoredMessages.add(nextMessage);
          log.info("The orderNumber of current flowNode is {}", nextFlowNode.getOrderNumber());
        } else {
          chatRepository.updateIsFinished(chat.getId(), true);
          var chatOptional = chatRepository.findById(chat.getId());

          if (chatOptional.isPresent()) {
            chat = chatOptional.get();
          }
          log.info("The chat with id {} is finished", chat.getId());
          break;
        }
      }
    }
    return CompletableFuture.completedFuture(new ChatDataDto(alreadyStoredMessages, new ChatParams(chat.getHearts())));
  }

  public void generateHintMessage(final String hintMessageId, final List<Message> actionableMsgs, final FlowNode hintNode,
                                  final Chat chat) {
    log.info("Try to got necessary info for generating hint");
    try {

      Prompt simulationHintPrompt =
        promptRepository.findFirstByNameOrderByIdDesc(PromptName.SIMULATION_MESSAGE_HINT)
          .orElseThrow();

      log.info("The prompt for hint message is working {}", simulationHintPrompt.isOn());

      var mockContent = "Glad to see you're continuing to practice your soft-skills";

      if (simulationHintPrompt.isOn()) {
        var updatedChat = chatRepository.findByIdWithMessages(chat.getId()).orElseThrow();

        log.info("Current thread name is {}", Thread.currentThread().getName());
        generateAiHintAsync(updatedChat, actionableMsgs, null, simulationHintPrompt)
          .thenAccept(aiRecommendation -> {
            log.info("The hint we got from ai is {}", aiRecommendation.map(MessageDto::content));
            String content = aiRecommendation.map(MessageDto::content)
              .orElse(mockContent);

            messageService.updateOrCreateHintMessage(
              hintMessageId,
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
          hintMessageId,
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
        var local = chat.getUser().getOrganization().getLocalization();
        var language = Objects.isNull(local) || local.isBlank() ? "UA" : local;

        boolean isOnboarding = msg.getFlowNode().getSimulation().getName().equals("Onboarding");

        var onboardingTitle = language.equalsIgnoreCase("UA") ? "Вперед до змін!" : "Forward to changes!";
        var simulationTitle = language.equalsIgnoreCase("UA") ? "Ваш результат" : "Your result";
        var title = isOnboarding ? onboardingTitle : simulationTitle;

        if (isOnboarding) {
          messageService.updateResultSimulationMessage(
            msg,
            null,
            title,
            language.equalsIgnoreCase("UA") ?
              "Приємно познайомитися. Йдемо відточувати навички справжнього спілкування!" :
              "Nice to meet you. Let's go to hone real communication skills!",
            RESULT_SIMULATION_CACHE
          );
          return;
        }

        var updatedChat = chatRepository.findByIdWithMessages(chat.getId()).orElseThrow();

        Prompt simulationRecommendationPrompt =
          promptRepository.findFirstByNameOrderByIdDesc(PromptName.SIMULATION_SUMMARY)
            .orElseThrow();

        var params = userHyperParameterService.findHyperParamsWithMaxValues(chat.getId())
          .stream()
          .map(param -> new UserHyperParamResponseDto(param.key(), param.value(), param.maxValue()))
          .toList();

        log.info("The params we got from user hyper parameters are {} and it size it {}", params, params.size());


        if (simulationRecommendationPrompt.isOn()) {
          var aiRecommendation = generateAiSummary(updatedChat, params, simulationRecommendationPrompt);

          log.info("The simulation recommendation we got from ai is {}", aiRecommendation.map(MessageDto::content));
          String content = aiRecommendation.map(MessageDto::content)
            .orElse(language.equalsIgnoreCase("UA") ?
                      "Радий бачити, що ви продовжуєте практикувати свої soft-skills" :
                      "Glad to see you're continuing to practice your soft-skills!");

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
            language.equalsIgnoreCase("UA") ?
              "Радий бачити, що ви продовжуєте практикувати свої soft-skills" :
              "Glad to see you're continuing to practice your soft-skills!",
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
    List<Message> messages = messageService.findQuestionUserMessageByOrderNumber(chatId, orderNumber);
    if (!messages.isEmpty()) {
      log.info("Messages {} found by order number: {}", messages, orderNumber);
      var message = messages.stream().max(Comparator.comparing(Message::getTimestamp));
      return interpreterMessageMapper.map(message.get());
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
        .preview(contentQuestion.getPreview())
        .messageType(contentQuestion.getMessageType())
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .responseTimeLimit(contentQuestion.getResponseTimeLimit())
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
        .hasHint(singleChoiceQuestion.isHasHint())
        .options(singleChoiceQuestion.getOptions())
        .responseTimeLimit(singleChoiceQuestion.getResponseTimeLimit())
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
        .hasHint(enterTextQuestion.isHasHint())
        .responseTimeLimit(enterTextQuestion.getResponseTimeLimit())
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
        .hasHint(singleChoiceTask.isHasHint())
        .responseTimeLimit(singleChoiceTask.getResponseTimeLimit())
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
        .responseTimeLimit(multipleChoiceQuestion.getResponseTimeLimit())
        .timestamp(LocalDateTime.now())
        .options(multipleChoiceQuestion.getOptions())
        .hasHint(multipleChoiceQuestion.isHasHint())
        .correct(multipleChoiceQuestion.getCorrect())
        .build();
    } else if (flowNode instanceof HintMessageNode hintMessageNode) {
      var local = chat.getUser().getOrganization().getLocalization();
      var language = Objects.isNull(local) || local.isBlank() ? "UA" : local;
      var title = language.equalsIgnoreCase("UA") ? "Лови підказку!" : "Tip";
      return HintMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .messageType(MessageType.HINT_MESSAGE)
        .flowNode(flowNode)
        .title(title)
        .character(flowNode.getCharacter())
        .role(ChatRole.APP)
        .timestamp(LocalDateTime.now())
        .build();
    } else if (flowNode instanceof ResultSimulationNode resultSimulationNode) {
      return LastSimulationMessage.builder()
        .id(UUID.randomUUID().toString())
        .chat(chat)
        .responseTimeLimit(resultSimulationNode.getResponseTimeLimit())
        .messageType(MessageType.RESULT_SIMULATION)
        .flowNode(flowNode)
        .character(flowNode.getCharacter())
        .role(ChatRole.APP)
        .timestamp(LocalDateTime.now())
        .build();
    }

    throw new RuntimeException("Please add converting type of messages from the flow");
  }

  public LastSimulationMessage generateLastSimulationMessage(Chat chat) {

    var local = chat.getUser().getOrganization().getLocalization();
    var language = Objects.isNull(local) || local.isBlank() ? "UA" : local;
    var lastMessage = LastSimulationMessage.builder()
      .id(UUID.randomUUID().toString())
      .chat(chat)
      .messageType(MessageType.RESULT_SIMULATION)
//      .character(flowNode.getCharacter())
      .content(language.equalsIgnoreCase("UA") ?
                 "На жаль, ви вичерпали всі можливі спроби. Спробуйте знову." :
                 "Unfortunately, you have exhausted all possible attempts. Try again.")
      .title(language.equalsIgnoreCase("UA") ? "Результат" : "Your result")
      .role(ChatRole.APP)
      .timestamp(LocalDateTime.now())
      .build();

    messageRepository.save(lastMessage);
    return lastMessage;
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
            onboardingStr,
            updatedChat.getUser().getOrganization().getLocalization()
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
        onboardingExtraction,
        updatedChat.getUser().getOrganization().getLocalization()
      ).get();
      log.error("everything is fine {} ", summary.content());
      return Optional.ofNullable(summary);

    } catch (Exception e) {
      log.error("Error while generating AI summary", e);
      return Optional.empty();
    }
  }

}
