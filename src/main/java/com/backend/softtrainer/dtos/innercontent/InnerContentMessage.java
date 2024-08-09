package com.backend.softtrainer.dtos.innercontent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class InnerContentMessage {

  private InnerContentMessageType type;

}
