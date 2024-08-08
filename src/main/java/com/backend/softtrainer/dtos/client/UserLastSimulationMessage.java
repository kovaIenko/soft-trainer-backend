package com.backend.softtrainer.dtos.client;

import com.backend.softtrainer.dtos.innercontent.InnerContentMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserLastSimulationMessage extends UserMessageDto {

  private List<InnerContentMessage> contents;

}
