package com.backend.softtrainer.dtos.innercontent;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class InnerContentMessage {

  private InnerContentMessageType type;

}
