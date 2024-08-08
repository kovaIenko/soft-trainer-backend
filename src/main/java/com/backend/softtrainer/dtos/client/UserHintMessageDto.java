package com.backend.softtrainer.dtos.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class UserHintMessageDto extends UserMessageDto {

  private String content;

  @Deprecated
  private String description;

  private String title;

}
