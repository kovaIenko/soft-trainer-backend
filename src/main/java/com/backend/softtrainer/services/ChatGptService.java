package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatDto;
import com.backend.softtrainer.dtos.MessageDto;
import com.backend.softtrainer.entities.Prompt;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.messages.ContentMessage;
import com.backend.softtrainer.entities.messages.EnterTextAnswerMessage;
import com.backend.softtrainer.entities.messages.EnterTextQuestionMessage;
import com.backend.softtrainer.entities.messages.HintMessage;
import com.backend.softtrainer.entities.messages.LastSimulationMessage;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskAnswerMessage;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceAnswerMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskAnswerMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.TextMessage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


public interface ChatGptService {
//
//  CompletableFuture<MessageDto> completeChat(final ChatDto chat);

//  CompletableFuture<MessageDto> buildSimulationSummary(final ChatDto chat, String prompt, Map<String, Double> params,
//                                                       final String userName, final String skillName,
//                                                       final String onboardingExtraction);

  CompletableFuture<MessageDto> buildAfterwardSimulationRecommendation(final ChatDto chat,
                                                                       final Prompt prompt,
                                                                       final Map<String, Double> params,
                                                                       final String skillName,
                                                                       final String onboardingExtraction,
                                                                       final String localization) throws
                                                                                                          InterruptedException;

  CompletableFuture<MessageDto> buildAfterwardActionableHintMessage(final ChatDto chat,
                                                                    final List<Message> actionableMessages,
                                                                    final Prompt prompt,
                                                                    final Map<String, Double> params,
                                                                    final String skillName,
                                                                    final String onboardingExtraction,
                                                                    final String localization) throws
                                                                                                       InterruptedException;

  static void convert(StringBuilder chatHistory, final Message message) {
    if (message instanceof EnterTextAnswerMessage msg) {
      chatHistory.append(" - ").append(getCharacterName(msg)).append(": ").append(msg.getContent()).append("\n");
    } else if (message instanceof EnterTextQuestionMessage msg) {
//      chatHistory.append(" ").append(getCharacterName(msg)).append(": ").append(msg.getContent());
    } else if (message instanceof TextMessage msg) {
      chatHistory.append(" - ").append(getCharacterName(msg)).append(": ").append(msg.getContent()).append("\n");
    } else if (message instanceof SingleChoiceQuestionMessage msg) {
      var options = "Options are: " + msg.getOptions() + " and correct (indexes): " + decreaseNumbers(msg.getCorrect());
      chatHistory.append(" - ").append(getCharacterName(msg)).append(": ").append(options);
    } else if (message instanceof SingleChoiceAnswerMessage msg) {
      var options = "Answer is: " + msg.getAnswer();
      chatHistory.append(" - ").append(getCharacterName(msg)).append(": ").append(options).append("\n");
    } else if (message instanceof SingleChoiceTaskQuestionMessage msg) {
      var options = "Options are: " + msg.getOptions() + " and correct (indexes): " + decreaseNumbers(msg.getCorrect());
      chatHistory.append(" - ").append(getCharacterName(msg)).append(": ").append(options);
    } else if (message instanceof SingleChoiceTaskAnswerMessage msg) {
      var options = "Answer is: " + msg.getAnswer();
      chatHistory.append(" - ").append(getCharacterName(msg)).append(": ").append(options).append("\n");
    } else if (message instanceof MultiChoiceTaskQuestionMessage msg) {
      var options = "Options are: " + msg.getOptions() + " and correct (indexes): " + decreaseNumbers(msg.getCorrect());
      chatHistory.append(" - ").append(getCharacterName(msg)).append(": ").append(options);
    } else if (message instanceof MultiChoiceTaskAnswerMessage msg) {
      var options = "Answer is: " + msg.getAnswer();
      chatHistory.append(" - ").append(getCharacterName(msg)).append(": ").append(options).append("\n");
    } else if (message instanceof HintMessage msg) {
      var options = "Підказка: " + msg.getContent();
      chatHistory.append(" - ").append(getCharacterName(msg)).append(": ").append(options).append("\n");
    } else if (message instanceof LastSimulationMessage msg) {
      var options = "Результат: " + msg.getContent();
      chatHistory.append(" - ").append(getCharacterName(msg)).append(": ").append(options).append("\n");
    } else if (message instanceof ContentMessage msg) {
//      var img = new ChatMessage.UserMessage.UserMessageWithContentParts.ContentPart.ImageContentPart.ImageUrl(
//        msg.getContent(),
//        Optional.empty()
//      );
//      var content =
//        List.of((ChatMessage.UserMessage.UserMessageWithContentParts.ContentPart)
//                  new ChatMessage.UserMessage.UserMessageWithContentParts.ContentPart.ImageContentPart(
//                    img));

//      chatHistory.append(msg.getCharacter().getName()).append(": ").append(content);
    } else {
      throw new IllegalArgumentException("Unknown message type: " + message.getClass());
    }

  }

  static String decreaseNumbers(String input) {
    // Handle null or empty input
    if (input == null || input.isEmpty()) {
      return input;
    }

    StringBuilder result = new StringBuilder();
    StringBuilder currentNumber = new StringBuilder();

    for (int i = 0; i < input.length(); i++) {
      char currentChar = input.charAt(i);

      if (Character.isDigit(currentChar)) {
        // Collect digits of the current number
        currentNumber.append(currentChar);
      } else {
        // If we have collected any number, decrease it and append
        if (currentNumber.length() > 0) {
          int number = Integer.parseInt(currentNumber.toString());
          result.append(number - 1);
          currentNumber.setLength(0);  // Reset for next number
        }
        // Append non-digit character
        result.append(currentChar);
      }
    }

    // Handle case where string ends with a number
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
