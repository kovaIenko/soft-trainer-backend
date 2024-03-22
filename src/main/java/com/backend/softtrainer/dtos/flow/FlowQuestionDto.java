package com.backend.softtrainer.dtos.flow;

import com.backend.softtrainer.entities.MessageType;
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
  @JsonSubTypes.Type(value = EnterTextQuestionDto.class, name = "enterTextQuestion"),
  @JsonSubTypes.Type(value = SingleChoiceQuestionDto.class, name = "singleChoiceQuestion"),
  @JsonSubTypes.Type(value = MultiChoiceQuestionDto.class, name = "multiChoiceTask"),
  @JsonSubTypes.Type(value = ContentQuestionDto.class, name = "contentQuestion"),
  @JsonSubTypes.Type(value = TextDto.class, name = "text")
})
@Data
@NoArgsConstructor
public abstract class FlowQuestionDto {

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

}
