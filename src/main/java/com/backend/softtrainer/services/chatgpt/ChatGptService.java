package com.backend.softtrainer.services.chatgpt;

import com.backend.softtrainer.dtos.ChatDto;
import com.backend.softtrainer.dtos.MessageDto;
import com.backend.softtrainer.entities.Prompt;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.messages.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public interface ChatGptService {


  CompletableFuture<MessageDto> classifyUserAnswer(
    EnterTextQuestionMessage message,
    Prompt prompt
  ) throws InterruptedException;

  CompletableFuture<MessageDto> buildAfterwardSimulationRecommendation(
    ChatDto chat,
    Prompt prompt,
    Map<String, Double> params,
    String skillName,
    String onboardingExtraction,
    String localization
  ) throws InterruptedException;

  CompletableFuture<MessageDto> buildAfterwardActionableHintMessage(
    ChatDto chat,
    List<Message> actionableMessages,
    Prompt prompt,
    Map<String, Double> params,
    String skillName,
    String onboardingExtraction,
    String localization
  ) throws InterruptedException;

  // 🟢 Refactored method to maintain compatibility with Java 17
  static void convert(StringBuilder chatHistory, Message message) {
    String formattedMessage = "";

    if (message instanceof EnterTextAnswerMessage msg) {
//      formattedMessage = formatMessage(msg, "User", msg.getContent());
    } else if (message instanceof EnterTextQuestionMessage msg) {
      if (Objects.nonNull(msg.getAnswer()) && !msg.getAnswer().isBlank()) {
        formattedMessage = formatMessage(msg, "User", msg.getAnswer());
      }
    } else if (message instanceof TextMessage msg) {
      formattedMessage = formatMessage(msg, getCharacterName(msg), msg.getContent());
    } else if (message instanceof SingleChoiceQuestionMessage msg) {
      if (Objects.nonNull(msg.getAnswer()) && !msg.getAnswer().isBlank()) {
        formattedMessage = formatOptions(msg);
      }
    } else if (message instanceof SingleChoiceAnswerMessage msg) {
//      formattedMessage = formatMessage(msg, "User", "Selected: " + msg.getAnswer());
    } else if (message instanceof MultiChoiceTaskQuestionMessage msg) {
      if (Objects.nonNull(msg.getAnswer()) && !msg.getAnswer().isBlank()) {
        formattedMessage = formatOptions(msg);
      }
    } else if (message instanceof MultiChoiceTaskAnswerMessage msg) {
//      formattedMessage = formatMessage(msg, "User", "Selected: " + msg.getAnswer());
    } else if (message instanceof HintMessage msg) {
      formattedMessage = formatMessage(msg, getCharacterName(msg), msg.getContent());
    } else if (message instanceof LastSimulationMessage msg) {
      formattedMessage = formatMessage(msg, "Simulation Summary", msg.getContent());
    } else if (message instanceof ContentMessage msg) {
      formattedMessage = formatMessage(msg, getCharacterName(msg), msg.getContent());
    } else {
      throw new IllegalArgumentException("Unknown message type: " + message.getClass());
    }

    chatHistory.append(formattedMessage).append("\n");
  }

  // 🟢 Helper function for structured message formatting
  private static String formatMessage(Message msg, String sender, String content) {
    return String.format("- %s: %s", sender, content);
  }

  // 🟢 Updated formatOptions method to support Java 17
  private static String formatOptions(Message msg) {
    if (msg instanceof SingleChoiceQuestionMessage singleChoiceMsg) {
      var question = String.format(
        "- AI-coordinator: Choose one option [%s] (Correct: %s)",
        singleChoiceMsg.getOptions(),
        decreaseNumbers(singleChoiceMsg.getCorrect())
      );
      var answer = formatMessage(msg, "User", "Selected: " + singleChoiceMsg.getAnswer());
      return question + "\n" + answer;
    } else if (msg instanceof MultiChoiceTaskQuestionMessage multiChoiceMsg) {
      var question = String.format(
        "- AI-coordinator: Choose multiple options [%s] (Correct: %s)",
        multiChoiceMsg.getOptions(),
        decreaseNumbers(multiChoiceMsg.getCorrect())
      );
      var answer = formatMessage(msg, "User", "Selected: " + multiChoiceMsg.getAnswer());
      return question + "\n" + answer;
    } else {
      throw new IllegalArgumentException("Unsupported message type for options formatting: " + msg.getClass());
    }
  }

  // 🟢 Keep number handling compatible with Java 17
  static String decreaseNumbers(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }

    StringBuilder result = new StringBuilder();
    StringBuilder currentNumber = new StringBuilder();

    for (int i = 0; i < input.length(); i++) {
      char currentChar = input.charAt(i);

      if (Character.isDigit(currentChar)) {
        currentNumber.append(currentChar);
      } else {
        if (currentNumber.length() > 0) {
          int number = Integer.parseInt(currentNumber.toString());
          result.append(number - 1);
          currentNumber.setLength(0); // Reset buffer
        }
        result.append(currentChar);
      }
    }

    if (currentNumber.length() > 0) {
      int number = Integer.parseInt(currentNumber.toString());
      result.append(number - 1);
    }

    return result.toString();
  }

  static String getCharacterName(Message message) {
    if (Objects.isNull(message.getCharacter())) {
      return "User";
    } else if (message.getRole().equals(ChatRole.USER)) {
      return "User";
    } else {
      return message.getCharacter().getName();
    }
  }
}
