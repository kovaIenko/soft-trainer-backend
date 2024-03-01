package com.backend.softtrainer.services;

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
import com.backend.softtrainer.repositories.FlowRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Optional;
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

  public boolean existsByName(final String name) {
    return flowRepository.existsByName(name);
  }

}
