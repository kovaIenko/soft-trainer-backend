package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatParams;
import com.backend.softtrainer.dtos.MessageAnswerOptionDto;
import com.backend.softtrainer.dtos.UserHyperParamResponseDto;
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
import com.backend.softtrainer.entities.messages.EnterTextQuestionMessage;
import com.backend.softtrainer.entities.messages.HintMessage;
import com.backend.softtrainer.entities.messages.LastSimulationMessage;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class UserMessageService {

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

  public List<UserMessageDto> combineMessages(final List<Message> messages, final ChatParams chatParams) {
    return messages.stream()
      .flatMap(msg -> convert(msg, chatParams))
      .filter(Objects::nonNull)
      .filter(msg -> !msg.getMessageType().equals(MessageType.HINT_MESSAGE))
      .peek(msg -> {
        if (Objects.nonNull(msg.getCharacter()) && msg.getCharacter().getFlowCharacterId() == -1) {
          msg.setCharacter(null);
        }
      })
      .sorted(Comparator.comparing(UserMessageDto::getTimestamp))
      .collect(Collectors.toList());
  }

  public List<UserMessageDto> combineMessage(final List<Message> messages, final ChatParams chatParams) {
    return messages.stream()
      .flatMap(msg -> convert(msg, chatParams))
      .filter(Objects::nonNull)
      .peek(msg -> {
        if (Objects.nonNull(msg.getCharacter()) && msg.getCharacter().getFlowCharacterId() == -1) {
          msg.setCharacter(null);
        }
      })
      .sorted(Comparator.comparing(UserMessageDto::getTimestamp))
      .collect(Collectors.toList());
  }

  private Double normalizeHyperParams(final Double value, final Double maxValue) {
    var ratio = value / maxValue;
    return 0.15 + 0.85 * (ratio < 0 ? 0 : ratio);
  }

  public Stream<UserMessageDto> convert(final Message message, final ChatParams chatParams) {

    if (Objects.isNull(message)) {
      return Stream.empty();
    }

    if (message instanceof TextMessage textMessage) {
      return Stream.of(UserTextMessageDto.builder()
                         .id(message.getId())
                         .timestamp(textMessage.getTimestamp())
                         .messageType(MessageType.TEXT)
                         .content(textMessage.getContent())
                         .character(textMessage.getCharacter())
                         .build());
    } else if (message instanceof LastSimulationMessage lastSimulationMessage) {

      var contents = new ArrayList<InnerContentMessage>();

      if (Objects.nonNull(lastSimulationMessage.getHyperParams()) && !lastSimulationMessage.getHyperParams().isEmpty()) {

        var params = lastSimulationMessage.getHyperParams();
        // not less than 3 params should have values
        if (lastSimulationMessage.getHyperParams().size() > 2) {

          var maxValue = Math.max(1.0, params.stream()
            .map(UserHyperParamResponseDto::value)
            .max(Double::compareTo)
            .orElse(1.0));


          var normalizedParams = params.stream()
            .map(param -> {
              //todo temporary
              Double maxValueFinal = Objects.isNull(param.maxValue()) || param.maxValue() == 0.0 ? maxValue : param.maxValue();
              return new UserHyperParamResponseDto(
                param.key(),
                normalizeHyperParams(param.value(), maxValueFinal),
                maxValueFinal
              );
            })
            .toList();

          var chartContent = ChartInnerContent.builder()
            .type(InnerContentMessageType.CHART)
            .values(normalizedParams)
            .build();
          contents.add(chartContent);
        }
      }


      if (Objects.nonNull(lastSimulationMessage.getContent()) && !lastSimulationMessage.getContent().isEmpty()) {
        // 🟢 Split content by "==" separator
        List<String> chunkedContent = List.of(lastSimulationMessage.getContent().split("=="));

        // 🟢 Convert each chunk into a structured message
        for (String chunk : chunkedContent) {
          var textContent = TextInnerContent.builder()
            .type(InnerContentMessageType.TEXT)
            .title(lastSimulationMessage.getTitle())
            .description(chunk.trim()) // Trim to remove extra spaces
            .build();
          contents.add(textContent);
        }
      }

      log.info("Last simulation message content {}", contents);

      return Stream.of(UserLastSimulationMessage.builder()
                         .timestamp(lastSimulationMessage.getTimestamp())
                         .messageType(MessageType.RESULT_SIMULATION)
                         .id(message.getId())
                         .character(lastSimulationMessage.getCharacter())
                         .contents(contents)
                         .hasHint(false)
                         .build());
    } else if (message instanceof HintMessage hintMessage) {
      var contents = new ArrayList<InnerContentMessage>();

      if (Objects.nonNull(hintMessage.getContent()) && !hintMessage.getContent().isEmpty()) {

        // 🟢 Split content by "==" separator
        List<String> chunkedContent = List.of(hintMessage.getContent().split("=="));

        // 🟢 Convert each chunk into a structured message
        for (String chunk : chunkedContent) {
          var textContent = TextInnerContent.builder()
            .type(InnerContentMessageType.TEXT)
            .title(hintMessage.getTitle())
            .description(chunk.trim()) // Trim to remove extra spaces
            .build();
          contents.add(textContent);
        }
      }

      log.info("Hint message content {}", contents);

      return Stream.of(UserHintMessageDto.builder()
                         .timestamp(hintMessage.getTimestamp())
                         .messageType(MessageType.HINT_MESSAGE)
                         .id(message.getId())
                         .character(hintMessage.getCharacter())
                         .contents(contents) // Store split content properly
                         .hasHint(false)
                         .build());
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
      return Stream.of(UserContentMessageDto.builder()
                         .timestamp(contentMessage.getTimestamp())
                         .messageType(contentMessage.getMessageType())
//        .previews(Objects.nonNull(contentMessage.getPreview()) ?
//                    List.of(contentMessage.getPreview().split(" \\|\\| ")) : Collections.emptyList())
                         .id(message.getId())
                         .urls(List.of(contentMessage.getContent().split(" \\|\\| ")))
                         .videos(videoObjects)
                         .character(contentMessage.getCharacter())
                         .build());
    } else if (message instanceof EnterTextQuestionMessage enterTextQuestionMessage) {
      if (Objects.nonNull(enterTextQuestionMessage.getAnswer()) && !enterTextQuestionMessage.getAnswer().isBlank()) {
        var correctnessState = CorrectnessState.UNDEFINED;
        if (Objects.nonNull(enterTextQuestionMessage.getOptions()) && !enterTextQuestionMessage.getOptions().isBlank()) {
          var options = split(enterTextQuestionMessage.getOptions());
          var userAnswer = splitToSet(enterTextQuestionMessage.getAnswer());
          var correctAnswer = splitCorrectAnswer(enterTextQuestionMessage.getCorrect());
          var answerOptions = getCorrectAnswerStr(options, correctAnswer);
          correctnessState = resolveCorrectnessState(answerOptions, userAnswer);

          if (enterTextQuestionMessage.isHasHint()) {
            adjustHeartsToCorrectnessOfAnswer(correctnessState, chatParams);
          }
        }
        return Stream.of(UserEnterTextMessageDto.builder()
                           .content(enterTextQuestionMessage.getContent())
                           .id(enterTextQuestionMessage.getId())
                           .idTemp(enterTextQuestionMessage.getId())
                           .timestamp(enterTextQuestionMessage.getTimestamp())
                           .hasHint(enterTextQuestionMessage.isHasHint())
                           .hintMessage(getHintMessage(enterTextQuestionMessage))
                           .messageType(MessageType.TEXT)
                           .correctness(correctnessState)
                           .isVoted(true)
                           .build());
      } else {
        return Stream.of(UserEnterTextMessageDto.builder()
                           .timestamp(enterTextQuestionMessage.getTimestamp())
                           .messageType(MessageType.ENTER_TEXT_QUESTION)
                           .id(message.getId())
                           .character(enterTextQuestionMessage.getCharacter())
                           .correctness(CorrectnessState.UNDEFINED)
                           .responseTimeLimit(enterTextQuestionMessage.getResponseTimeLimit())
                           .content(enterTextQuestionMessage.getContent())
                           .isVoted(false)
                           .build());
      }
    } else if (message instanceof SingleChoiceQuestionMessage singleChoiceQuestionMessage) {

      if (Objects.nonNull(singleChoiceQuestionMessage.getAnswer()) && !singleChoiceQuestionMessage.getAnswer().isBlank()) {
        var options = split(singleChoiceQuestionMessage.getOptions());
        var userAnswer = splitToSet(singleChoiceQuestionMessage.getAnswer());
        var correctAnswer = splitCorrectAnswer(singleChoiceQuestionMessage.getCorrect());
        var answerOptions = getCorrectAnswerStr(options, correctAnswer);
        var correctnessState = resolveCorrectnessState(answerOptions, userAnswer);

        if (singleChoiceQuestionMessage.isHasHint()) {
          adjustHeartsToCorrectnessOfAnswer(correctnessState, chatParams);
        }
        return Stream.of(UserSingleChoiceMessageDto.builder()
                           .answer(singleChoiceQuestionMessage.getAnswer())
                           .options(convertOptions(
                             singleChoiceQuestionMessage.getOptions(),
                             singleChoiceQuestionMessage.getAnswer()
                           ))
                           .timestamp(singleChoiceQuestionMessage.getTimestamp())
                           .id(singleChoiceQuestionMessage.getId())
                           .idTemp(singleChoiceQuestionMessage.getId())
                           .content(singleChoiceQuestionMessage.getAnswer())
                           .hasHint(singleChoiceQuestionMessage.isHasHint())
                           .hintMessage(getHintMessage(singleChoiceQuestionMessage))
                           .correctness(correctnessState)
                           .messageType(MessageType.TEXT)
                           .isVoted(true)
                           .build());
      } else {
        return Stream.of(UserSingleChoiceMessageDto.builder()
                           .options(convertOptions(singleChoiceQuestionMessage.getOptions(), ""))
                           .timestamp(singleChoiceQuestionMessage.getTimestamp())
                           .messageType(MessageType.SINGLE_CHOICE_QUESTION)
                           .id(message.getId())
                           .hasHint(singleChoiceQuestionMessage.isHasHint())
                           .correctness(CorrectnessState.UNDEFINED)
                           .hintMessage(getHintMessage(singleChoiceQuestionMessage))
                           .responseTimeLimit(singleChoiceQuestionMessage.getResponseTimeLimit())
                           .isVoted(false)
                           .build());
      }
    } else if (message instanceof SingleChoiceTaskQuestionMessage singleChoiceTaskQuestionMessage) {

      if (Objects.nonNull(singleChoiceTaskQuestionMessage.getAnswer()) && !singleChoiceTaskQuestionMessage.getAnswer()
        .isBlank()) {
        var options = split(singleChoiceTaskQuestionMessage.getOptions());
        var userAnswer = splitToSet(singleChoiceTaskQuestionMessage.getAnswer());
        var correctAnswer = splitCorrectAnswer(singleChoiceTaskQuestionMessage.getCorrect());
        var answerOptions = getCorrectAnswerStr(options, correctAnswer);
        var correctnessState = resolveCorrectnessState(answerOptions, userAnswer);

        if (singleChoiceTaskQuestionMessage.isHasHint()) {
          adjustHeartsToCorrectnessOfAnswer(correctnessState, chatParams);
        }

        return Stream.of(UserSingleChoiceTaskMessageDto.builder()
                           .options(convertOptions(
                             singleChoiceTaskQuestionMessage.getOptions(),
                             singleChoiceTaskQuestionMessage.getAnswer()
                           ))
                           .answer(singleChoiceTaskQuestionMessage.getAnswer())
                           .timestamp(singleChoiceTaskQuestionMessage.getTimestamp())
                           .id(message.getId())
                           .messageType(MessageType.SINGLE_CHOICE_TASK)
                           .hasHint(singleChoiceTaskQuestionMessage.isHasHint())
                           .correctness(correctnessState)
                           .hintMessage(getHintMessage(singleChoiceTaskQuestionMessage))
                           .responseTimeLimit(singleChoiceTaskQuestionMessage.getResponseTimeLimit())
                           .isVoted(true)
                           .build());
      } else {
        return Stream.of(UserSingleChoiceTaskMessageDto.builder()
                           .options(convertOptions(singleChoiceTaskQuestionMessage.getOptions(), ""))
                           .timestamp(singleChoiceTaskQuestionMessage.getTimestamp())
                           .id(message.getId())
                           .messageType(MessageType.SINGLE_CHOICE_TASK)
                           .hasHint(singleChoiceTaskQuestionMessage.isHasHint())
                           .correctness(CorrectnessState.UNDEFINED)
                           .hintMessage(getHintMessage(singleChoiceTaskQuestionMessage))
                           .responseTimeLimit(singleChoiceTaskQuestionMessage.getResponseTimeLimit())
                           .isVoted(false)
                           .build());

      }
    } else if (message instanceof MultiChoiceTaskQuestionMessage multiChoiceTaskQuestionMessage) {

      if (Objects.nonNull(multiChoiceTaskQuestionMessage.getAnswer()) && !multiChoiceTaskQuestionMessage.getAnswer().isBlank()) {

        var options = split(multiChoiceTaskQuestionMessage.getOptions());
        var userAnswer = splitToSet(multiChoiceTaskQuestionMessage.getAnswer());
        var correctAnswer = splitCorrectAnswer(multiChoiceTaskQuestionMessage.getCorrect());
        var answerOptions = getCorrectAnswerStr(options, correctAnswer);
        var correctnessState = resolveCorrectnessState(answerOptions, userAnswer);

        if (multiChoiceTaskQuestionMessage.isHasHint()) {
          adjustHeartsToCorrectnessOfAnswer(correctnessState, chatParams);
        }

        var msgs = userAnswer.stream()
          .filter(answerOption -> Objects.nonNull(answerOption) && !answerOption.isBlank())
          .map(userAnswerOption ->
                 UserSingleChoiceMessageDto.builder()
                   .content(userAnswerOption)
                   .timestamp(multiChoiceTaskQuestionMessage.getTimestamp())
                   .messageType(MessageType.TEXT)
                   .id(multiChoiceTaskQuestionMessage.getId())
                   .correctness(resolveCorrectnessState(answerOptions, splitToSet(userAnswerOption)))
                   .isVoted(true)
                   .build())
          .map(a -> (UserMessageDto) a)
          .toList();
        if (!msgs.isEmpty()) {
          var lastMsg = msgs.get(msgs.size() - 1);

          if (multiChoiceTaskQuestionMessage.isHasHint()) {
            lastMsg.setHintMessage(getHintMessage(multiChoiceTaskQuestionMessage));
            lastMsg.setHasHint(multiChoiceTaskQuestionMessage.isHasHint());
          }
        }
        return msgs.stream();
      } else {
        return Stream.of(UserMultiChoiceTaskMessageDto.builder()
                           .options(convertOptions(multiChoiceTaskQuestionMessage.getOptions(), ""))
                           .timestamp(multiChoiceTaskQuestionMessage.getTimestamp())
                           .id(message.getId())
                           .messageType(MessageType.MULTI_CHOICE_TASK)
                           .correctness(CorrectnessState.UNDEFINED)
                           .hasHint(multiChoiceTaskQuestionMessage.isHasHint())
                           .responseTimeLimit(multiChoiceTaskQuestionMessage.getResponseTimeLimit())
                           .hintMessage(getHintMessage(multiChoiceTaskQuestionMessage))
                           .isVoted(false)
                           .build());
      }
    }
    throw new NoSuchElementException("The incorrect question type " + message.getClass().getName());
  }

  private UserHintMessageDto getHintMessage(final Message message) {
    if (message.isHasHint()) {
      var converted = convert(message.getHintMessage(), null).toList();
      log.info("The hint message for the message {} is {}", message.getId(), converted);
      if (converted.isEmpty()) {
        return null;
      } else {
        return (UserHintMessageDto) converted.get(0);
      }
    } else {
      return null;
    }
  }

}
