package com.backend.softtrainer.dtos.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
public class UserContentMessageDto extends UserMessageDto {

  private List<String> urls;

}
