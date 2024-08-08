package com.backend.softtrainer.dtos.client;

import com.backend.softtrainer.dtos.innercontent.InnerContentMessage;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
public class UserHintMessageDto extends UserMessageDto {

  private List<InnerContentMessage> contents;

}
