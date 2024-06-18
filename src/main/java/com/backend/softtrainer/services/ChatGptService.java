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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ChatGptService {

  CompletableFuture<MessageDto> completeChat(final ChatDto chat);

  CompletableFuture<MessageDto> buildSimulationSummary(final ChatDto chat, String prompt, Map<String, Double> params, final String userName, final String skillName);

  default ChatMessage convert(final Message message) {
    if (message instanceof EnterTextMessage msg) {
      return ChatMessage.userMessage(msg.getContent());
    } else if (message instanceof TextMessage msg) {
      return ChatMessage.userMessage(msg.getContent());
    } else if (message instanceof SingleChoiceQuestionMessage msg) {
      var options = "Options are : " + msg.getOptions();
      return ChatMessage.systemMessage(options);
    } else if (message instanceof SingleChoiceAnswerMessage msg) {
      var options = "Answers are: " + msg.getAnswer();
      return ChatMessage.userMessage(options);
    } else if (message instanceof SingleChoiceTaskQuestionMessage msg) {
      var options = "Options are: " + msg.getOptions();
      return ChatMessage.systemMessage(options);
    } else if (message instanceof SingleChoiceTaskAnswerMessage msg) {
      var options = "Answers are: " + msg.getAnswer();
      return ChatMessage.userMessage(options);
    } else if (message instanceof MultiChoiceTaskQuestionMessage msg) {
      var options = "Options are: " + msg.getOptions();
      return ChatMessage.systemMessage(options);
    } else if (message instanceof MultiChoiceTaskAnswerMessage msg) {
      var options = "Answers are: " + msg.getAnswer();
      return ChatMessage.userMessage(options);
    } else if (message instanceof ContentMessage msg) {
      var img = new ChatMessage.UserMessage.UserMessageWithContentParts.ContentPart.ImageContentPart.ImageUrl(
        msg.getUrl(),
        Optional.empty()
      );
      var content =
        List.of((ChatMessage.UserMessage.UserMessageWithContentParts.ContentPart)
                  new ChatMessage.UserMessage.UserMessageWithContentParts.ContentPart.ImageContentPart(
        img));
      return new ChatMessage.UserMessage.UserMessageWithContentParts(content, Optional.empty());
    } else {
      throw new IllegalArgumentException("Unknown message type: " + message.getClass());
    }

  }
}
