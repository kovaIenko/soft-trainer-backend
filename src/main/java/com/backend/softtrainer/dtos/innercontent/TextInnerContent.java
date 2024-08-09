package com.backend.softtrainer.dtos.innercontent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TextInnerContent extends InnerContentMessage {

  private String title;

  private String description;

}
