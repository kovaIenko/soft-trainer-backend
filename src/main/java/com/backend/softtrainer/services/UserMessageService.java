package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatParams;
import com.backend.softtrainer.dtos.MessageAnswerOptionDto;
import com.backend.softtrainer.dtos.client.CorrectnessState;
import com.backend.softtrainer.dtos.client.UserContentMessageDto;
import com.backend.softtrainer.dtos.client.UserEnterTextMessageDto;
import com.backend.softtrainer.dtos.client.UserHintMessageDto;
import com.backend.softtrainer.dtos.client.UserLastSimulationMessage;
import com.backend.softtrainer.dtos.client.UserMessageDto;
import com.backend.softtrainer.dtos.client.UserMultiChoiceTaskMessageDto;
import com.backend.softtrainer.dtos.client.UserSingleChoiceMessageDto;
import com.backend.softtrainer.dtos.client.UserSingleChoiceTaskMessageDto;
import com.backend.softtrainer.dtos.client.UserTextMessageDto;
import com.backend.softtrainer.dtos.client.VideoObjDto;
import com.backend.softtrainer.dtos.innercontent.ChartInnerContent;
import com.backend.softtrainer.dtos.innercontent.InnerContentMessage;
import com.backend.softtrainer.dtos.innercontent.InnerContentMessageType;
import com.backend.softtrainer.dtos.innercontent.TextInnerContent;
import com.backend.softtrainer.entities.enums.MessageType;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class UserMessageService {

  private static List<Class<? extends Message>> QUESTION_CLASSES = List.of(
    SingleChoiceTaskQuestionMessage.class,
    SingleChoiceQuestionMessage.class,
    MultiChoiceTaskQuestionMessage.class,
    EnterTextQuestionMessage.class
  );

  private List<MessageAnswerOptionDto> convertOptions(String options, final String answer) {
    var answers = Stream.of(answer.split("\\|\\|"))
      .filter(Objects::nonNull)
      .map(String::trim)
      .map(String::toLowerCase)
      .collect(Collectors.toSet());

    return Stream.of(options.split("\\|\\|")).map(option -> {
        var modifiedOption = option.trim();
        var messageBuilder = MessageAnswerOptionDto.builder()
          .optionId(UUID.randomUUID().toString())
          .text(modifiedOption);

        if (answers.contains(modifiedOption.toLowerCase())) {
          messageBuilder.isSelected(true);
        }
        return messageBuilder;
      })
      .map(MessageAnswerOptionDto.MessageAnswerOptionDtoBuilder::build)
      .toList();
  }

  private CorrectnessState resolveCorrectnessState(HashSet<String> correct,
                                                   HashSet<String> userAnswer) {
// If both sets are exactly equal
    if (correct.equals(userAnswer)) {
      return CorrectnessState.CORRECT;
    }

    // Create a copy of the correct set to calculate intersection
    Set<String> intersection = new java.util.HashSet<>(correct);
    intersection.retainAll(userAnswer);  // Keep only elements that are in both sets

    // If there are no common elements, it's incorrect
    if (intersection.isEmpty()) {
      return CorrectnessState.INCORRECT;
    }

    // Calculate the size of the intersection (correct matches)
    int correctMatches = intersection.size();
    int correctSize = correct.size();

    // Calculate the match percentage
    double matchPercentage = (double) correctMatches / correctSize;

    // If the match percentage is 50% or more, it's partially correct
    if (matchPercentage >= 0.5) {
      return CorrectnessState.PARTIALLY_CORRECT;
    } else {
      // Otherwise, it's partially incorrect
      return CorrectnessState.PARTIALLY_INCORRECT;
    }
  }

  private List<String> split(final String text) {
    return Stream.of(text.split("\\|\\|"))
      .map(String::trim)
      .collect(Collectors.toList());
  }

  private HashSet<String> splitToSet(final String text) {
    return Stream.of(text.split("\\|\\|"))
      .map(String::trim)
      .collect(Collectors.toCollection(HashSet::new));
  }

  private List<Integer> splitCorrectAnswer(final String text) {
    return Stream.of(text.split("\\|\\|"))
      .map(String::trim)
      .map(Integer::parseInt)
      .collect(Collectors.toList());
  }

  private HashSet<String> getCorrectAnswerStr(final List<String> options,
                                              final List<Integer> correctAnswer) {
    return correctAnswer.stream()
      .map(index -> options.get(index - 1))
      .collect(Collectors.toCollection(HashSet::new));
  }

  private void adjustHeartsToCorrectnessOfAnswer(CorrectnessState correctnessState, ChatParams chatParams) {
    if (Objects.nonNull(chatParams.getHearts())) {
      switch (correctnessState) {
        case CORRECT:
//          chatParams.setHearts(chatParams.getHearts() + 1);
          break;
        case PARTIALLY_CORRECT:
//          chatParams.setHearts(chatParams.getHearts() + 0.5);
          break;
        case INCORRECT:
          chatParams.setHearts(chatParams.getHearts() - 1);
          break;
        case PARTIALLY_INCORRECT:
          chatParams.setHearts(chatParams.getHearts() - 0.5);
          break;
        default:
          break;
      }
    }
  }

  public Stream<UserMessageDto> combine(final Message question, final Message answer, final ChatParams chatParams) {
    if (question instanceof SingleChoiceTaskQuestionMessage singleChoiceTaskQuestionMessage
      && answer instanceof SingleChoiceTaskAnswerMessage singleChoiceTaskAnswerMessage) {
      var options = split(singleChoiceTaskQuestionMessage.getOptions());
      var userAnswer = splitToSet(singleChoiceTaskAnswerMessage.getAnswer());
      var correctAnswer = splitCorrectAnswer(singleChoiceTaskQuestionMessage.getCorrect());
      var answerOptions = getCorrectAnswerStr(options, correctAnswer);
      var correctnessState = resolveCorrectnessState(answerOptions, userAnswer);

      adjustHeartsToCorrectnessOfAnswer(correctnessState, chatParams);

      return Stream.of(UserSingleChoiceTaskMessageDto.builder()
                         .answer(singleChoiceTaskAnswerMessage.getAnswer())
                         .options(convertOptions(
                           singleChoiceTaskQuestionMessage.getOptions(),
                           singleChoiceTaskAnswerMessage.getAnswer()
                         ))
                         .timestamp(singleChoiceTaskAnswerMessage.getTimestamp())
                         .messageType(MessageType.SINGLE_CHOICE_TASK)
                         .id(singleChoiceTaskAnswerMessage.getId())
                         .idTemp(singleChoiceTaskQuestionMessage.getId())
                         .hasHint(singleChoiceTaskAnswerMessage.isHasHint())
                         .hintMessage(getHitMessage(singleChoiceTaskAnswerMessage))
                         .correctness(correctnessState)
                         .isVoted(true)
                         .build());
    } else if (question instanceof SingleChoiceQuestionMessage singleChoiceQuestionMessage
      && answer instanceof SingleChoiceAnswerMessage singleChoiceAnswerMessage) {

      var options = split(singleChoiceQuestionMessage.getOptions());
      var userAnswer = splitToSet(singleChoiceAnswerMessage.getAnswer());
      var correctAnswer = splitCorrectAnswer(singleChoiceQuestionMessage.getCorrect());
      var answerOptions = getCorrectAnswerStr(options, correctAnswer);
      var correctnessState = resolveCorrectnessState(answerOptions, userAnswer);

      adjustHeartsToCorrectnessOfAnswer(correctnessState, chatParams);

      return Stream.of(UserSingleChoiceMessageDto.builder()
                         .answer(singleChoiceAnswerMessage.getAnswer())
                         .options(convertOptions(singleChoiceQuestionMessage.getOptions(), singleChoiceAnswerMessage.getAnswer()))
                         .timestamp(singleChoiceAnswerMessage.getTimestamp())
                         .id(singleChoiceAnswerMessage.getId())
                         .idTemp(singleChoiceQuestionMessage.getId())
                         .content(singleChoiceAnswerMessage.getAnswer())
                         .hasHint(singleChoiceAnswerMessage.isHasHint())
                         .hintMessage(getHitMessage(singleChoiceAnswerMessage))
                         .correctness(correctnessState)
                         .messageType(MessageType.TEXT)
                         .isVoted(true)
                         .build());
    } else if (question instanceof MultiChoiceTaskQuestionMessage multiChoiceTaskQuestionMessage
      && answer instanceof MultiChoiceTaskAnswerMessage multiChoiceTaskAnswerMessage) {
      var options = split(multiChoiceTaskQuestionMessage.getOptions());
      var userAnswer = splitToSet(multiChoiceTaskAnswerMessage.getAnswer());
      var correctAnswer = splitCorrectAnswer(multiChoiceTaskQuestionMessage.getCorrect());
      var answerOptions = getCorrectAnswerStr(options, correctAnswer);
      var correctnessState = resolveCorrectnessState(answerOptions, userAnswer);

      adjustHeartsToCorrectnessOfAnswer(correctnessState, chatParams);

      return userAnswer.stream()
        .filter(answerOption -> Objects.nonNull(answerOption) && !answerOption.isBlank())
        .map(userAnswerOption ->
               UserSingleChoiceMessageDto.builder()
                 .content(userAnswerOption)
                 .timestamp(multiChoiceTaskAnswerMessage.getTimestamp())
                 .messageType(MessageType.TEXT)
                 .id(multiChoiceTaskAnswerMessage.getId())
                 .hasHint(multiChoiceTaskAnswerMessage.isHasHint())
                 .hintMessage(getHitMessage(multiChoiceTaskAnswerMessage))
                 .correctness(resolveCorrectnessState(answerOptions, splitToSet(userAnswerOption)))
                 .isVoted(true)
                 .build());
    } else if (question instanceof EnterTextQuestionMessage enterQuestionTextMessage
      && answer instanceof EnterTextAnswerMessage enterAnswerTextMessage) {
      return Stream.of(UserEnterTextMessageDto.builder()
                         .content(enterAnswerTextMessage.getContent())
                         .id(enterAnswerTextMessage.getId())
                         .idTemp(enterQuestionTextMessage.getId())
                         .timestamp(enterAnswerTextMessage.getTimestamp())
                         .correctness(CorrectnessState.UNDEFINED)
                         .messageType(MessageType.TEXT)
                         .isVoted(true)
                         .build());
    }
    throw new NoSuchElementException("The incorrect pair of question and answer for chat " + question.getChat()
      .getId() + " Question " + question.getMessageType() + " id: " + question.getId() + " , answer " + answer.getMessageType() + " id: " + answer.getId());
  }

  public List<UserMessageDto> combineMessages(final List<Message> messages, final ChatParams chatParams) {

    var messagesGroupedByFlowNodes = messages.stream()
      .collect(Collectors.groupingBy(message -> Optional.ofNullable(message.getFlowNode())));

    return messagesGroupedByFlowNodes.values().stream().flatMap(collection -> sortCombinedMessages(chatParams, collection))
      .filter(Objects::nonNull)
      .sorted(Comparator.comparing(UserMessageDto::getTimestamp))
      .peek(msg -> {
        //todo the stupid thing
        if (Objects.nonNull(msg.getCharacter())) {
          if (msg.getCharacter().getFlowCharacterId() == -1) {
            msg.setCharacter(null);
          }
        }
      })
      .collect(Collectors.toList());
  }

  private @Nullable Stream<UserMessageDto> sortCombinedMessages(final ChatParams chatParams, final List<Message> collection) {
    if (collection.size() == 2) {
      return QUESTION_CLASSES.contains(collection.get(0).getClass()) ?
        combine(collection.get(0), collection.get(1), chatParams) : combine(collection.get(1), collection.get(0), chatParams);
    } else if (collection.size() == 1) {
      //todo temp
      if (collection.get(0).getMessageType().equals(MessageType.HINT_MESSAGE)) {
        return null;
      }
      return Stream.of(convert(collection.get(0)));
    } else {
      log.info(String.format(
        "Collection size while combining messages by flow node is %s, the collection is %s",
        collection.size(),
        collection
      ));
      return null;
    }
  }

  public List<UserMessageDto> combineOneTypeMessages(final List<Message> messages, ChatParams chatParams) {

    var messagesGroupedByFlowNodes = messages.stream()
      .collect(Collectors.groupingBy(message -> Optional.ofNullable(message.getFlowNode())));

    return messagesGroupedByFlowNodes.values().stream()
      .flatMap(collection -> {
        if (collection.size() == 2) {
          return QUESTION_CLASSES.contains(collection.get(0).getClass()) ?
            combine(collection.get(0), collection.get(1), chatParams) : combine(
            collection.get(1),
            collection.get(0),
            chatParams
          );
        } else if (collection.size() == 1) {
          return Stream.of(convert(collection.get(0)));
        } else {
          log.info(String.format(
            "Collection size while combining messages by flow node is %s, the collection is %s",
            collection.size(),
            collection
          ));
          return null;
        }
      })
      .filter(Objects::nonNull)
      .sorted(Comparator.comparing(UserMessageDto::getTimestamp))
      .peek(msg -> {
        //todo the stupid thing
        if (Objects.nonNull(msg.getCharacter())) {
          if (msg.getCharacter().getFlowCharacterId() == -1) {
            msg.setCharacter(null);
          }
        }
      })
      .collect(Collectors.toList());
  }

  public UserMessageDto convert(final Message message) {

    if (Objects.isNull(message)) {
      return null;
    }

    if (message instanceof TextMessage textMessage) {
      return UserTextMessageDto.builder()
        .id(message.getId())
        .timestamp(textMessage.getTimestamp())
        .messageType(MessageType.TEXT)
        .content(textMessage.getContent())
        .character(textMessage.getCharacter())
        .build();
    } else if (message instanceof LastSimulationMessage lastSimulationMessage) {

      var contents = new ArrayList<InnerContentMessage>();

      if (Objects.nonNull(lastSimulationMessage.getHyperParams()) && !lastSimulationMessage.getHyperParams().isEmpty()) {
        var chartContent = ChartInnerContent.builder()
          .type(InnerContentMessageType.CHART)
          .values(lastSimulationMessage.getHyperParams())
          .build();

        contents.add(chartContent);
      }

      if (Objects.nonNull(lastSimulationMessage.getContent()) && !lastSimulationMessage.getContent().isEmpty()) {
        var textContent = TextInnerContent.builder()
          .type(InnerContentMessageType.TEXT)
          .description(lastSimulationMessage.getContent())
          .title(lastSimulationMessage.getTitle())
          .build();
        contents.add(textContent);
      }

      log.info("Last simulation message content {}", contents);

      return UserLastSimulationMessage.builder()
        .timestamp(lastSimulationMessage.getTimestamp())
        .messageType(MessageType.RESULT_SIMULATION)
        .id(message.getId())
        .character(lastSimulationMessage.getCharacter())
        .contents(contents)
        .hasHint(false)
        .build();
    } else if (message instanceof HintMessage hintMessage) {

      var contents = new ArrayList<InnerContentMessage>();

      if (Objects.nonNull(hintMessage.getContent()) && !hintMessage.getContent().isEmpty()) {
        var textContent = TextInnerContent.builder()
          .type(InnerContentMessageType.TEXT)
          .title(hintMessage.getTitle())
          .description(hintMessage.getContent())
          .build();
        contents.add(textContent);
      }

      log.info("Hint message content {}", contents);

      return UserHintMessageDto.builder()
        .timestamp(hintMessage.getTimestamp())
        .messageType(MessageType.HINT_MESSAGE)
        .id(message.getId())
        .character(hintMessage.getCharacter())
        .contents(contents)
        .hasHint(false)
        .build();
    } else if (message instanceof ContentMessage contentMessage) {

      List<VideoObjDto> videoObjects = new ArrayList<>();

      //todo temporary
      if (contentMessage.getMessageType().equals(MessageType.VIDEOS)) {

        List<String> previews = Objects.nonNull(contentMessage.getPreview()) ? List.of(contentMessage.getPreview()
                                                                                         .split(" \\|\\| ")) :
          Collections.emptyList();
        List<String> urls = Objects.nonNull(contentMessage.getContent()) ? List.of(contentMessage.getContent()
                                                                                     .split(" \\|\\| ")) :
          Collections.emptyList();

        urls.forEach(url -> {
          String pr = previews.size() > urls.indexOf(url) ? previews.get(urls.indexOf(url)) : null;
          videoObjects.add(new VideoObjDto(url, pr));
        });
      }
      return UserContentMessageDto.builder()
        .timestamp(contentMessage.getTimestamp())
        .messageType(contentMessage.getMessageType())
//        .previews(Objects.nonNull(contentMessage.getPreview()) ?
//                    List.of(contentMessage.getPreview().split(" \\|\\| ")) : Collections.emptyList())
        .id(message.getId())
        .urls(List.of(contentMessage.getContent().split(" \\|\\| ")))
        .videos(videoObjects)
        .character(contentMessage.getCharacter())
        .build();
    } else if (message instanceof EnterTextQuestionMessage enterTextQuestionMessage) {
      return UserEnterTextMessageDto.builder()
        .timestamp(enterTextQuestionMessage.getTimestamp())
        .messageType(MessageType.ENTER_TEXT_QUESTION)
        .id(message.getId())
        .character(enterTextQuestionMessage.getCharacter())
        .correctness(CorrectnessState.UNDEFINED)
        .responseTimeLimit(enterTextQuestionMessage.getResponseTimeLimit())
        .content(enterTextQuestionMessage.getContent())
        .isVoted(false)
        .build();
    } else if (message instanceof SingleChoiceQuestionMessage singleChoiceQuestionMessage) {
      return UserSingleChoiceMessageDto.builder()
        .options(convertOptions(singleChoiceQuestionMessage.getOptions(), ""))
        .timestamp(singleChoiceQuestionMessage.getTimestamp())
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .id(message.getId())
        .hasHint(singleChoiceQuestionMessage.isHasHint())
        .correctness(CorrectnessState.UNDEFINED)
        .hintMessage(getHitMessage(singleChoiceQuestionMessage))
        .responseTimeLimit(singleChoiceQuestionMessage.getResponseTimeLimit())
        .isVoted(false)
        .build();
    } else if (message instanceof SingleChoiceTaskQuestionMessage singleChoiceTaskQuestionMessage) {
      return UserSingleChoiceTaskMessageDto.builder()
        .options(convertOptions(singleChoiceTaskQuestionMessage.getOptions(), ""))
        .timestamp(singleChoiceTaskQuestionMessage.getTimestamp())
        .id(message.getId())
        .messageType(MessageType.SINGLE_CHOICE_TASK)
        .hasHint(singleChoiceTaskQuestionMessage.isHasHint())
        .correctness(CorrectnessState.UNDEFINED)
        .hintMessage(getHitMessage(singleChoiceTaskQuestionMessage))
        .responseTimeLimit(singleChoiceTaskQuestionMessage.getResponseTimeLimit())
        .isVoted(false)
        .build();
    } else if (message instanceof MultiChoiceTaskQuestionMessage multiChoiceTaskQuestionMessage) {
      return UserMultiChoiceTaskMessageDto.builder()
        .options(convertOptions(multiChoiceTaskQuestionMessage.getOptions(), ""))
        .timestamp(multiChoiceTaskQuestionMessage.getTimestamp())
        .id(message.getId())
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .correctness(CorrectnessState.UNDEFINED)
        .hasHint(multiChoiceTaskQuestionMessage.isHasHint())
        .responseTimeLimit(multiChoiceTaskQuestionMessage.getResponseTimeLimit())
        .hintMessage(getHitMessage(multiChoiceTaskQuestionMessage))

        .isVoted(false)
        .build();
    }
    throw new NoSuchElementException("The incorrect question type " + message.getClass().getName());
  }

//  private UserLastSimulationMessage createDumpLastSimulationMessage(){
//
//    var contents = new ArrayList<InnerContentMessage>();
//
//    if (Objects.nonNull(lastSimulationMessage.getHyperParams()) && !lastSimulationMessage.getHyperParams().isEmpty()) {
//      var chartContent = ChartInnerContent.builder()
//        .type(InnerContentMessageType.CHART)
//        .values(lastSimulationMessage.getHyperParams())
//        .build();
//
//      contents.add(chartContent);
//    }
//
//    if (Objects.nonNull(lastSimulationMessage.getContent()) && !lastSimulationMessage.getContent().isEmpty()) {
//      var textContent = TextInnerContent.builder()
//        .type(InnerContentMessageType.TEXT)
//        .description(lastSimulationMessage.getContent())
//        .title(lastSimulationMessage.getTitle())
//        .build();
//      contents.add(textContent);
//    }
//
//    log.info("Last simulation message content {}", contents);
//
//    return UserLastSimulationMessage.builder()
//      .timestamp(LocalDateTime.now())
//      .messageType(MessageType.RESULT_SIMULATION)
//      .id(message.getId())
//      .character(lastSimulationMessage.getCharacter())
//      .contents(contents)
//      .hasHint(false)
//      .build();
//  }

  private UserHintMessageDto getHitMessage(final Message message) {
    if (message.isHasHint()) {
      var converted = convert(message.getHintMessage());
      if (Objects.isNull(converted)) {
        return null;
      } else {
        return (UserHintMessageDto) converted;
      }
    } else {
      return null;
    }
  }

}
