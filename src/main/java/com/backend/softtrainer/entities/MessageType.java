package com.backend.softtrainer.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@AllArgsConstructor
public enum MessageType {

  TEXT("text", false),
  SINGLE_CHOICE_QUESTION("singleChoiceQuestion", true),
  MULTI_CHOICE_QUESTION("multiChoiceTask", true),
  CONTENT_QUESTION("contentQuestion", false),
  ENTER_TEXT_QUESTION("enterTextQuestion", true);

  private final String value;
  private final boolean actionable;

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

  public static List<String> getActionableMessageTypes() {
    return Stream.of(values())
      .filter(a -> a.actionable)
      .map(Enum::name)
      .map(String::toUpperCase)
      .toList();
  }

}
