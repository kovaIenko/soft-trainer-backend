package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatDto;
import com.backend.softtrainer.dtos.MessageDto;
import com.backend.softtrainer.entities.messages.ContentMessage;
import com.backend.softtrainer.entities.messages.EnterTextMessage;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskAnswerMessage;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceAnswerMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskAnswerMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.TextMessage;
import io.github.stefanbratanov.jvm.openai.ChatMessage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ChatGptService {

  CompletableFuture<MessageDto> completeChat(final ChatDto chat);

  CompletableFuture<MessageDto> buildSimulationSummary(final ChatDto chat, String prompt, Map<String, Double> params,
                                                       final String userName, final String skillName);

  default void convert(StringBuilder chatHistory, final Message message) {
    if (message instanceof EnterTextMessage msg) {
      chatHistory.append(msg.getCharacter().getName()).append(" : ").append(msg.getContent());
    } else if (message instanceof TextMessage msg) {
      chatHistory.append(getCharacterName(msg)).append(" : ").append(msg.getContent());
    } else if (message instanceof SingleChoiceQuestionMessage msg) {
      var options = "Options are : " + msg.getOptions();
      chatHistory.append(getCharacterName(msg)).append(" : ").append(options);
    } else if (message instanceof SingleChoiceAnswerMessage msg) {
      var options = "Answers are: " + msg.getAnswer();
      chatHistory.append(getCharacterName(msg)).append(" : ").append(options);
    } else if (message instanceof SingleChoiceTaskQuestionMessage msg) {
      var options = "Options are: " + msg.getOptions();
      chatHistory.append(getCharacterName(msg)).append(" : ").append(options);
    } else if (message instanceof SingleChoiceTaskAnswerMessage msg) {
      var options = "Answers are: " + msg.getAnswer();
      chatHistory.append(getCharacterName(msg)).append(" : ").append(options);
    } else if (message instanceof MultiChoiceTaskQuestionMessage msg) {
      var options = "Options are: " + msg.getOptions();
      chatHistory.append(getCharacterName(msg)).append(" : ").append(options);
    } else if (message instanceof MultiChoiceTaskAnswerMessage msg) {
      var options = "Answers are: " + msg.getAnswer();
      chatHistory.append(getCharacterName(msg)).append(" : ").append(options);
    } else if (message instanceof ContentMessage msg) {
      var img = new ChatMessage.UserMessage.UserMessageWithContentParts.ContentPart.ImageContentPart.ImageUrl(
        msg.getUrl(),
        Optional.empty()
      );
      var content =
        List.of((ChatMessage.UserMessage.UserMessageWithContentParts.ContentPart)
                  new ChatMessage.UserMessage.UserMessageWithContentParts.ContentPart.ImageContentPart(
                    img));

//      chatHistory.append(msg.getCharacter().getName()).append(" : ").append(content);
    } else {
      throw new IllegalArgumentException("Unknown message type: " + message.getClass());
    }

  }

  default String getCharacterName(Message message) {
    if (Objects.isNull(message.getCharacter())) {
      return "User";
    } else {
      return message.getCharacter().getName();
    }
  }
}
