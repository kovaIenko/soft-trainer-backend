package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatDto;
import com.backend.softtrainer.dtos.MessageDto;
import io.github.stefanbratanov.jvm.openai.ChatClient;
import io.github.stefanbratanov.jvm.openai.ChatMessage;
import io.github.stefanbratanov.jvm.openai.CreateChatCompletionRequest;
import io.github.stefanbratanov.jvm.openai.OpenAI;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class ChatGptServiceJvmOpenAi implements ChatGptService {

  private final OpenAI openAI = OpenAI.newBuilder(System.getenv("OPEN_AI_SECRET_KEY"))
    .requestTimeout(Duration.ofSeconds(10))
    .build();

  private final ChatClient chatClient = openAI.chatClient();

  @Override
  public CompletableFuture<MessageDto> completeChat(final ChatDto chat) {

    var messages = chat.messages().stream()
      .map(MessageDto::content)
      .map(content -> (ChatMessage) new ChatMessage.UserMessage.UserMessageWithTextContent(content, Optional.empty())) // Explicitly use the correct constructor
      .toList();

    CreateChatCompletionRequest request = CreateChatCompletionRequest.newBuilder()
      .model("gpt-3.5-turbo")
      .messages(messages)
      .build();

    return chatClient.createChatCompletionAsync(request)
      .thenApply(chatCompletion -> {
        if (!chatCompletion.choices().isEmpty()) {
          return new MessageDto(chatCompletion.choices().get(0).message().content());
        }
        return new MessageDto("empty");
      });
  }

}
