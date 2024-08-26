package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.MessageAnswerOptionDto;
import com.backend.softtrainer.dtos.client.UserContentMessageDto;
import com.backend.softtrainer.dtos.client.UserEnterTextMessageDto;
import com.backend.softtrainer.dtos.client.UserHintMessageDto;
import com.backend.softtrainer.dtos.client.UserLastSimulationMessage;
import com.backend.softtrainer.dtos.client.UserMessageDto;
import com.backend.softtrainer.dtos.client.UserMultiChoiceTaskMessageDto;
import com.backend.softtrainer.dtos.client.UserSingleChoiceMessageDto;
import com.backend.softtrainer.dtos.client.UserSingleChoiceTaskMessageDto;
import com.backend.softtrainer.dtos.client.UserTextMessageDto;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

  public UserMessageDto combine(final Message question, final Message answer) {

    if (question instanceof SingleChoiceTaskQuestionMessage singleChoiceTaskQuestionMessage
      && answer instanceof SingleChoiceTaskAnswerMessage singleChoiceTaskAnswerMessage) {
      return UserSingleChoiceTaskMessageDto.builder()
        .answer(singleChoiceTaskAnswerMessage.getAnswer())
        .options(convertOptions(singleChoiceTaskQuestionMessage.getOptions(), singleChoiceTaskAnswerMessage.getAnswer()))
        .timestamp(singleChoiceTaskAnswerMessage.getTimestamp())
        .messageType(MessageType.SINGLE_CHOICE_TASK)
        .id(singleChoiceTaskAnswerMessage.getId())
        .idTemp(singleChoiceTaskQuestionMessage.getId())
        .isVoted(true)
        .build();
    } else if (question instanceof SingleChoiceQuestionMessage singleChoiceQuestionMessage
      && answer instanceof SingleChoiceAnswerMessage singleChoiceAnswerMessage) {
      return UserSingleChoiceMessageDto.builder()
        .answer(singleChoiceAnswerMessage.getAnswer())
        .options(convertOptions(singleChoiceQuestionMessage.getOptions(), singleChoiceAnswerMessage.getAnswer()))
        .timestamp(singleChoiceAnswerMessage.getTimestamp())
        .id(singleChoiceAnswerMessage.getId())
        .idTemp(singleChoiceQuestionMessage.getId())
        .content(singleChoiceAnswerMessage.getAnswer())
        .messageType(MessageType.TEXT)
        .isVoted(true)
        .build();
    } else if (question instanceof MultiChoiceTaskQuestionMessage multiChoiceTaskQuestionMessage
      && answer instanceof MultiChoiceTaskAnswerMessage multiChoiceTaskAnswerMessage) {
      return UserMultiChoiceTaskMessageDto.builder()
        .answer(multiChoiceTaskAnswerMessage.getAnswer())
        .options(convertOptions(multiChoiceTaskQuestionMessage.getOptions(), multiChoiceTaskAnswerMessage.getAnswer()))
        .timestamp(multiChoiceTaskAnswerMessage.getTimestamp())
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .id(multiChoiceTaskAnswerMessage.getId())
        .idTemp(multiChoiceTaskQuestionMessage.getId())
        .isVoted(true)
        .build();
    } else if (question instanceof EnterTextQuestionMessage enterQuestionTextMessage
      && answer instanceof EnterTextAnswerMessage enterAnswerTextMessage) {
      return UserEnterTextMessageDto.builder()
        .content(enterAnswerTextMessage.getContent())
        .id(enterAnswerTextMessage.getId())
        .idTemp(enterQuestionTextMessage.getId())
        .timestamp(enterAnswerTextMessage.getTimestamp())
        .messageType(MessageType.TEXT)
        .isVoted(true)
        .build();
    }
    throw new NoSuchElementException("The incorrect pair of question and answer for chat " + question.getChat()
      .getId() + " Question " + question.getMessageType() + " id: " + question.getId() + " , answer " + answer.getMessageType() + " id: " + answer.getId());
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
        .build();
    } else if (message instanceof ContentMessage contentMessage) {
      return UserContentMessageDto.builder()
        .timestamp(contentMessage.getTimestamp())
        .messageType(contentMessage.getMessageType())
        .id(message.getId())
        .urls(List.of(contentMessage.getContent().split(" \\|\\| ")))
        .character(contentMessage.getCharacter())
        .build();
    } else if (message instanceof EnterTextQuestionMessage enterTextQuestionMessage) {
      return UserEnterTextMessageDto.builder()
        .timestamp(enterTextQuestionMessage.getTimestamp())
        .messageType(MessageType.ENTER_TEXT_QUESTION)
        .id(message.getId())
        .character(enterTextQuestionMessage.getCharacter())
        .content(enterTextQuestionMessage.getContent())
        .build();
    } else if (message instanceof SingleChoiceQuestionMessage singleChoiceQuestionMessage) {
      return UserSingleChoiceMessageDto.builder()
        .options(convertOptions(singleChoiceQuestionMessage.getOptions(), ""))
        .timestamp(singleChoiceQuestionMessage.getTimestamp())
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .id(message.getId())
        .isVoted(false)
        .build();
    } else if (message instanceof SingleChoiceTaskQuestionMessage singleChoiceTaskQuestionMessage) {
      return UserSingleChoiceTaskMessageDto.builder()
        .options(convertOptions(singleChoiceTaskQuestionMessage.getOptions(), ""))
        .timestamp(singleChoiceTaskQuestionMessage.getTimestamp())
        .id(message.getId())
        .messageType(MessageType.SINGLE_CHOICE_TASK)
        .isVoted(false)
        .build();
    } else if (message instanceof MultiChoiceTaskQuestionMessage multiChoiceTaskQuestionMessage) {
      return UserMultiChoiceTaskMessageDto.builder()
        .options(convertOptions(multiChoiceTaskQuestionMessage.getOptions(), ""))
        .timestamp(multiChoiceTaskQuestionMessage.getTimestamp())
        .id(message.getId())
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .isVoted(false)
        .build();
    }
    throw new NoSuchElementException("The incorrect question type " + message.getClass().getName());
  }

}
