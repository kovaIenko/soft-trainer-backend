package com.backend.softtrainer.dtos.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class UserContentMessageDto extends UserMessageDto {

  private String url;

}
