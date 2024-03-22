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
  @JsonSubTypes.Type(value = EnterTextAnswerMessageDto.class, name = "enterTextAnswer"),
  @JsonSubTypes.Type(value = SingleChoiceAnswerMessageDto.class, name = "singleChoiceAnswer"),
  @JsonSubTypes.Type(value = MultiChoiceAnswerMessageDto.class, name = "multiChoiceAnswer"),
})
@Data
@NoArgsConstructor
public class MessageRequestDto {

  @JsonProperty("owner_id")
  private Long ownerId;

  private LocalDateTime timestamp;

  @JsonProperty("chat_id")
  private Long chatId;

  @JsonProperty("message_type")
  private MessageType type;

}
