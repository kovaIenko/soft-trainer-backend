package com.backend.softtrainer.dtos.flow;

import com.backend.softtrainer.entities.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "message_type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = EnterTextQuestionDto.class, name = "EnterTextQuestion"),
  @JsonSubTypes.Type(value = SingleChoiceQuestionDto.class, name = "SingleChoiceQuestion"),
  @JsonSubTypes.Type(value = SingleChoiceTaskDto.class, name = "SingleChoiceTask"),
  @JsonSubTypes.Type(value = MultiChoiceTaskDto.class, name = "MultiChoiceTask"),
  @JsonSubTypes.Type(value = ImagesDto.class, name = "Image"),
  @JsonSubTypes.Type(value = VideosDto.class, name = "Video"),
  @JsonSubTypes.Type(value = TextDto.class, name = "Text"),
  @JsonSubTypes.Type(value = HintMessageDto.class, name = "HintMessage"),
  @JsonSubTypes.Type(value = ResultSimulationDto.class, name = "ResultSimulation")
})
@Data
@NoArgsConstructor
public abstract class FlowNodeDto {

  @JsonProperty("message_id")
  @NotNull
  private long messageId;

  @JsonProperty(value = "previous_message_id", required = true)
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  @NotNull
  private List<Long> previousOrderNumber;

  @JsonProperty(value = "message_type", required = true)
  @NotNull
  private MessageType messageType;

  @JsonProperty("show_predicate")
  @NotNull
  private String showPredicate;

  @NotNull
  @JsonProperty("character_id")
  private long author;

  @JsonProperty("has_hint")
  private boolean hasHint;

  @JsonProperty("response_time_limit")
  private long responseTimeLimit;

}
