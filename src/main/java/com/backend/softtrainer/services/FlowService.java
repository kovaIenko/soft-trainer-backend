package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.CharacterDto;
import com.backend.softtrainer.dtos.FlowRequestDto;
import com.backend.softtrainer.dtos.flow.ContentQuestionDto;
import com.backend.softtrainer.dtos.flow.EnterTextQuestionDto;
import com.backend.softtrainer.dtos.flow.FlowQuestionDto;
import com.backend.softtrainer.dtos.flow.MultiChoiceQuestionDto;
import com.backend.softtrainer.dtos.flow.SingleChoiceQuestionDto;
import com.backend.softtrainer.dtos.flow.TextDto;
import com.backend.softtrainer.entities.MessageType;
import com.backend.softtrainer.entities.flow.ContentQuestion;
import com.backend.softtrainer.entities.flow.EnterTextQuestion;
import com.backend.softtrainer.entities.flow.FlowQuestion;
import com.backend.softtrainer.entities.flow.MultipleChoiceQuestion;
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion;
import com.backend.softtrainer.entities.flow.Text;
import com.backend.softtrainer.repositories.CharacterRepository;
import com.backend.softtrainer.repositories.FlowRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.backend.softtrainer.entities.Character;

@Service
@AllArgsConstructor
public class FlowService {

  private FlowRepository flowRepository;

  private CharacterRepository characterRepository;

  public void uploadFlow(final FlowRequestDto flowRequestDto) {

    Map<Long, Character> characterMap = flowRequestDto.getCharacters().stream()
      .collect(Collectors.toMap(
        CharacterDto::id, // Key extractor
        characterDto -> Character.builder()
          .name(characterDto.name())
          .avatar(characterDto.avatar())
          .flowCharacterId(characterDto.id())
          .build(), // Value function
        (existing, replacement) -> existing, // Merge function, in case of duplicate keys
        HashMap::new // Map supplier
      ));

    characterRepository.saveAll(characterMap.values());

    var flowRecords =
      flowRequestDto.getFlow()
        .stream()
        .flatMap(flowQuestionDto -> this.convert(flowQuestionDto, characterMap.get(flowQuestionDto.getAuthor())))
        .map(a -> {
          a.setName(flowRequestDto.getName());
          return a;
        }).toList();
    flowRepository.saveAll(flowRecords);
  }

  //todo stupid violation of second SOLID
  private Stream<FlowQuestion> convert(final FlowQuestionDto flowRecordDto, final Character authorEntity) {
    return flowRecordDto.getPreviousOrderNumber()
      .stream()
      .map(prevMessageId -> convertFlow(flowRecordDto, prevMessageId, authorEntity));
  }

  private FlowQuestion convertFlow(final FlowQuestionDto flowRecordDto, final long previousMessageId,  final Character authorEntity) {

    if (flowRecordDto instanceof ContentQuestionDto contentQuestionDto) {
      return ContentQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .url(contentQuestionDto.getUrl())
        .character(authorEntity)
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.CONTENT_QUESTION)
        .build();
    } else if (flowRecordDto instanceof EnterTextQuestionDto enterTextQuestionDto) {
      return EnterTextQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .prompt(enterTextQuestionDto.getPrompt())
        .character(authorEntity)
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.ENTER_TEXT_QUESTION)
        .build();
    } else if (flowRecordDto instanceof TextDto textDto) {
      return Text.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .text(textDto.getText())
        .character(authorEntity)
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.TEXT)
        .build();
    } else if (flowRecordDto instanceof SingleChoiceQuestionDto singleChoiceQuestionDto) {
      return SingleChoiceQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .character(authorEntity)
        .correct(singleChoiceQuestionDto.getCorrect())
        .options(String.join(" || ", singleChoiceQuestionDto.getOptions()))
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .build();
    } else if (flowRecordDto instanceof MultiChoiceQuestionDto multipleChoiceQuestionDto) {
      return MultipleChoiceQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .character(authorEntity)
        .correct(String.join(" || ", multipleChoiceQuestionDto.getCorrect()))
        .options(String.join(" || ", multipleChoiceQuestionDto.getOptions()))
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.MULTI_CHOICE_QUESTION)
        .build();
    }
    throw new NoSuchElementException();
  }

  public Optional<FlowQuestion> getRootFlowTask(final String name) {
    return flowRepository.findFlowTaskByPreviousOrderNumberAndName(0L, name);
  }

  public List<FlowQuestion> getFirstFlowQuestionsUntilActionable(final String name) {

    var actionableMessageTypes = MessageType.getActionableMessageTypes();

    List<FlowQuestion> questions = flowRepository.findFirst10QuestionsByName(name)
      .stream().
      sorted(Comparator.comparing(FlowQuestion::getOrderNumber))
      .toList();

    List<FlowQuestion> result = new ArrayList<>();

    for (FlowQuestion question : questions) {
      result.add(question); // Always add the current question

      if (actionableMessageTypes.contains(question.getMessageType().name())) {
        break; // Stop the loop as we've included the first actionable question
      }
    }
    return result;
  }

  public boolean existsByName(final String name) {
    return flowRepository.existsByName(name);
  }

  public Set<String> getAllNameFlows() {
    return flowRepository.findAllNameFlows();
  }

  public List<FlowQuestion> findAllByPreviousOrderNumber(final long previousOrderNumber){
    return flowRepository.findAllByPreviousOrderNumber(previousOrderNumber);
  }

}
