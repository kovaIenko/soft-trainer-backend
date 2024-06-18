package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.MessageAnswerOptionDto;
import com.backend.softtrainer.dtos.client.UserContentMessageDto;
import com.backend.softtrainer.dtos.client.UserEnterTextMessageDto;
import com.backend.softtrainer.dtos.client.UserLastSimulationMessage;
import com.backend.softtrainer.dtos.client.UserMessageDto;
import com.backend.softtrainer.dtos.client.UserMultiChoiceTaskMessageDto;
import com.backend.softtrainer.dtos.client.UserSingleChoiceMessageDto;
import com.backend.softtrainer.dtos.client.UserSingleChoiceTaskMessageDto;
import com.backend.softtrainer.dtos.client.UserTextMessageDto;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.messages.ContentMessage;
import com.backend.softtrainer.entities.messages.EnterTextMessage;
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
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class UserMessageService {

  private static List<Class<? extends Message>> QUESTION_CLASSES = List.of(
    SingleChoiceTaskQuestionMessage.class,
    SingleChoiceQuestionMessage.class,
    MultiChoiceTaskQuestionMessage.class
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

  public UserMessageDto combine(final Message question, final Message answer) {

    if (question instanceof SingleChoiceTaskQuestionMessage singleChoiceTaskQuestionMessage
      && answer instanceof SingleChoiceTaskAnswerMessage singleChoiceTaskAnswerMessage) {
      return UserSingleChoiceTaskMessageDto.builder()
        .answer(singleChoiceTaskAnswerMessage.getAnswer())
        .options(convertOptions(singleChoiceTaskQuestionMessage.getOptions(), singleChoiceTaskAnswerMessage.getAnswer()))
        .timestamp(singleChoiceTaskAnswerMessage.getTimestamp())
        .messageType(MessageType.SINGLE_CHOICE_TASK)
        .isVoted(true)
        .build();
    } else if (question instanceof SingleChoiceQuestionMessage singleChoiceQuestionMessage
      && answer instanceof SingleChoiceAnswerMessage singleChoiceAnswerMessage) {
      return UserSingleChoiceMessageDto.builder()
        .answer(singleChoiceAnswerMessage.getAnswer())
        .options(convertOptions(singleChoiceQuestionMessage.getOptions(), singleChoiceAnswerMessage.getAnswer()))
        .timestamp(singleChoiceAnswerMessage.getTimestamp())
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .isVoted(true)
        .build();
    } else if (question instanceof MultiChoiceTaskQuestionMessage multiChoiceTaskQuestionMessage
      && answer instanceof MultiChoiceTaskAnswerMessage multiChoiceTaskAnswerMessage) {
      return UserMultiChoiceTaskMessageDto.builder()
        .answer(multiChoiceTaskAnswerMessage.getAnswer())
        .options(convertOptions(multiChoiceTaskQuestionMessage.getOptions(), multiChoiceTaskAnswerMessage.getAnswer()))
        .timestamp(multiChoiceTaskAnswerMessage.getTimestamp())
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .isVoted(true)
        .build();
    }
    throw new NoSuchElementException("The incorrect pair of question and answer for chat " + question.getChat().getId() + " Question id: " + question.getId() + " , answer"
                                       + " id: " + answer.getId());
  }

  public List<UserMessageDto> combineMessages(final List<Message> messages) {

    var messagesGroupedByFlowNodes = messages.stream()
      .collect(Collectors.groupingBy(message -> Optional.ofNullable(message.getFlowNode())));

    return messagesGroupedByFlowNodes.values().stream()
      .map(collection -> {
        if (collection.size() == 2) {
          return QUESTION_CLASSES.contains(collection.get(0).getClass()) ?
            combine(collection.get(0), collection.get(1)) : combine(collection.get(1), collection.get(0));
        } else if (collection.size() == 1) {
          return convert(collection.get(0));
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
        if (Objects.nonNull(msg.getCharacter())) {
          if (msg.getCharacter().getName().equalsIgnoreCase("user")) {
            msg.setCharacter(null);
          }
        }
      })
      .collect(Collectors.toList());
  }

  public UserMessageDto convert(final Message message) {
    if (message instanceof TextMessage textMessage) {
      return UserTextMessageDto.builder()
        .timestamp(textMessage.getTimestamp())
        .messageType(MessageType.TEXT)
        .content(textMessage.getContent())
        .character(textMessage.getCharacter())
        .build();
    } else if (message instanceof LastSimulationMessage lastSimulationMessage) {
      return UserLastSimulationMessage.builder()
        .timestamp(lastSimulationMessage.getTimestamp())
        .messageType(MessageType.RESULT_SIMULATION)
        .nextSimulationId(lastSimulationMessage.getNextSimulationId())
        .hyperParams(lastSimulationMessage.getHyperParams())
        .aiSummary(lastSimulationMessage.getAiSummary())
        .build();
    } else if (message instanceof ContentMessage contentMessage) {
      return UserContentMessageDto.builder()
        .timestamp(contentMessage.getTimestamp())
        .messageType(MessageType.CONTENT_QUESTION)
        .url(contentMessage.getUrl())
        .character(contentMessage.getCharacter())
        .build();
    } else if (message instanceof EnterTextMessage enterTextMessage) {
      return UserEnterTextMessageDto.builder()
        .timestamp(enterTextMessage.getTimestamp())
        .messageType(MessageType.ENTER_TEXT_QUESTION)
        .character(enterTextMessage.getCharacter())
        .content(enterTextMessage.getContent())
        .build();
    } else if (message instanceof SingleChoiceQuestionMessage singleChoiceQuestionMessage) {
      return UserSingleChoiceMessageDto.builder()
        .options(convertOptions(singleChoiceQuestionMessage.getOptions(), ""))
        .timestamp(singleChoiceQuestionMessage.getTimestamp())
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .isVoted(false)
        .build();
    } else if (message instanceof SingleChoiceTaskQuestionMessage singleChoiceTaskQuestionMessage) {
      return UserSingleChoiceTaskMessageDto.builder()
        .options(convertOptions(singleChoiceTaskQuestionMessage.getOptions(), ""))
        .timestamp(singleChoiceTaskQuestionMessage.getTimestamp())
        .messageType(MessageType.SINGLE_CHOICE_TASK)
        .isVoted(false)
        .build();
    } else if (message instanceof MultiChoiceTaskQuestionMessage multiChoiceTaskQuestionMessage) {
      return UserMultiChoiceTaskMessageDto.builder()
        .options(convertOptions(multiChoiceTaskQuestionMessage.getOptions(), ""))
        .timestamp(multiChoiceTaskQuestionMessage.getTimestamp())
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .isVoted(false)
        .build();
    }
    throw new NoSuchElementException("The incorrect question type");
  }

}
