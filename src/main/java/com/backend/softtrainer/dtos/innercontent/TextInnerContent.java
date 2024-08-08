package com.backend.softtrainer.dtos.innercontent;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class TextInnerContent extends InnerContentMessage {

  private String title;

  private String description;

}
