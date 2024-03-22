package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.FlowRequestDto;
import com.backend.softtrainer.dtos.flow.*;
import com.backend.softtrainer.entities.MessageType;
import com.backend.softtrainer.entities.flow.*;
import com.backend.softtrainer.repositories.FlowRepository;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class FlowService {

  private FlowRepository flowRepository;

  public void uploadFlow(final FlowRequestDto flowRequestDto) {
    var flowRecords =
      flowRequestDto.getFlow().stream().flatMap(this::convert)
        .map(a -> {
          a.setName(flowRequestDto.getName());
          return a;
        }).toList();
    flowRepository.saveAll(flowRecords);
  }

  //todo stupid violation of second SOLID
  private Stream<FlowQuestion> convert(final FlowQuestionDto flowRecordDto) {
    return flowRecordDto.getPreviousOrderNumber()
      .stream()
      .map(prevMessageId -> convertFlow(flowRecordDto, prevMessageId));
  }

  private FlowQuestion convertFlow(final FlowQuestionDto flowRecordDto, final long previousMessageId) {

    if (flowRecordDto instanceof ContentQuestionDto contentQuestionDto) {
      return ContentQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .url(contentQuestionDto.getUrl())
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.CONTENT_QUESTION)
        .build();
    } else if (flowRecordDto instanceof EnterTextQuestionDto enterTextQuestionDto) {
      return EnterTextQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .prompt(enterTextQuestionDto.getPrompt())
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.ENTER_TEXT_QUESTION)
        .build();
    } else if (flowRecordDto instanceof TextDto textDto) {
      return Text.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .text(textDto.getText())
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.TEXT)
        .build();
    } else if (flowRecordDto instanceof SingleChoiceQuestionDto singleChoiceQuestionDto) {
      return SingleChoiceQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .correct(singleChoiceQuestionDto.getCorrect())
        .options(String.join(" || ", singleChoiceQuestionDto.getOptions()))
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .build();
    } else if (flowRecordDto instanceof MultiChoiceQuestionDto multipleChoiceQuestionDto) {
      return MultipleChoiceQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
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

  public List<FlowQuestion> findAllByPreviousOrderNumber(final long previousOrderNumber) {
    return flowRepository.findAllByPreviousOrderNumber(previousOrderNumber);
  }

  @NotNull
  public List<FlowQuestion> findByOrderNumber(final long orderNumber) {
    return flowRepository.findAllByOrderNumber(orderNumber);
  }

  public FlowQuestion save(final FlowQuestion flowQuestion){
    return flowRepository.save(flowQuestion);
  }
}
