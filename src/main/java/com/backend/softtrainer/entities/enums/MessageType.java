package com.backend.softtrainer.entities.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@AllArgsConstructor
public enum MessageType {

  TEXT("Text", false),
  SINGLE_CHOICE_QUESTION("SingleChoiceQuestion", true),
  SINGLE_CHOICE_TASK("SingleChoiceTask", true),
  MULTI_CHOICE_TASK("MultiChoiceQuestion", true),

  @Deprecated
  CONTENT_QUESTION("ContentQuestion", false),
  IMAGES("Images", false),
  VIDEOS("Videos", false),

  ENTER_TEXT_QUESTION("EnterTextQuestion", true),

  RESULT_SIMULATION("ResultSimulation", false),

  HINT_MESSAGE("HintMessage", false),

  @Deprecated
  ENTER_TEXT_ANSWER("enterTextAnswer", false),
  @Deprecated
  SINGLE_CHOICE_ANSWER("singleChoiceAnswer", false),
  @Deprecated
  MULTI_CHOICE_ANSWER("multiChoiceAnswer", false);

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
