package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageAnswerOptionDto {

  @JsonProperty("option_id")
  private String optionId;

  @JsonProperty("text")
  private String text;

  @JsonProperty("is_selected")
  private boolean isSelected;
}
