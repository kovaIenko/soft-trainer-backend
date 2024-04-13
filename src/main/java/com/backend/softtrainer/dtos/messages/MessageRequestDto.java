package com.backend.softtrainer.dtos.messages;

import com.backend.softtrainer.entities.MessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "message_type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = EnterTextAnswerMessageDto.class, name = "EnterTextQuestion"),
  @JsonSubTypes.Type(value = SingleChoiceAnswerMessageDto.class, name = "SingleChoiceQuestion"),
  @JsonSubTypes.Type(value = SingleChoiceTaskAnswerMessageDto.class, name = "SingleChoiceTask"),
  @JsonSubTypes.Type(value = MultiChoiceTaskAnswerMessageDto.class, name = "MultiChoiceQuestion"),
})
@Data
@NoArgsConstructor
public class MessageRequestDto {

  @JsonProperty("owner_id")
  private Long ownerId;

  @Deprecated
  private LocalDateTime timestamp;

  @JsonProperty("chat_id")
  private Long chatId;

  @JsonProperty("message_type")
  private MessageType type;

}
