package com.backend.softtrainer.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public enum MessageType {

  TEXT("text"),
  SINGLE_CHOICE_QUESTION("singleChoiceQuestion"),
  MULTI_CHOICE_QUESTION("multiChoiceTask"),
  CONTENT_QUESTION("contentQuestion"),
  ENTER_TEXT_QUESTION("enterTextQuestion");

  private final String value;

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static MessageType fromValue(final String value) {
    for (MessageType type : values()) {
      if (type.getValue().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown enum type " + value + ", Allowed values are " + Arrays.toString(MessageType.values()));
  }

}
