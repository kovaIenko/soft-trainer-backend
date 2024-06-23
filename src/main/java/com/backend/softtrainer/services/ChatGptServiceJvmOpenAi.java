package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatDto;
import com.backend.softtrainer.dtos.MessageDto;
import io.github.stefanbratanov.jvm.openai.ChatClient;
import io.github.stefanbratanov.jvm.openai.ChatMessage;
import io.github.stefanbratanov.jvm.openai.CreateChatCompletionRequest;
import io.github.stefanbratanov.jvm.openai.OpenAI;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ChatGptServiceJvmOpenAi implements ChatGptService {

  private final OpenAI openAI = OpenAI.newBuilder(System.getenv("OPEN_AI_SECRET_KEY"))
    .requestTimeout(Duration.ofSeconds(10))
    .build();

  private final ChatClient chatClient = openAI.chatClient();

  private final String gptModel = "gpt-4-turbo";

//  private final ChatClient client = openAI


  @Override
  public CompletableFuture<MessageDto> completeChat(final ChatDto chat) {

//    var messages = chat.messages().stream()
//      .map(MessageDto::content)
//      .map(content -> (ChatMessage) new ChatMessage.UserMessage.UserMessageWithTextContent(
//        content,
//        Optional.empty()
//      )) // Explicitly use the correct constructor
//      .toList();
//
//    CreateChatCompletionRequest request = CreateChatCompletionRequest.newBuilder()
//      .model("gpt-4o")
//      .messages(messages)
//      .build();
//
//    return chatClient.createChatCompletionAsync(request)
//      .thenApply(chatCompletion -> {
//        if (!chatCompletion.choices().isEmpty()) {
//          return new MessageDto(chatCompletion.choices().get(0).message().content());
//        }
//
//        //todo better handling for that
//        return new MessageDto("empty");
//      });

    throw new NotImplementedException("Please implement that method");
  }

  public CompletableFuture<MessageDto> buildSimulationSummary(final ChatDto chat, String template, Map<String, Double> params,
                                                              final String userName, final String skillName) {

    String userParams = params.entrySet().stream()
      .map(entry -> {
        var key = entry.getKey();
        var value = entry.getValue();
        return key + " = " + value;
      })
      .collect(Collectors.joining(" \n "));

    System.out.println("user params: " + userParams);

    var chatHistory = new StringBuilder();
    chat.messages().forEach(message -> convert(chatHistory, message));

    System.out.println("chat history: " + chatHistory);

    var promptMessage = String.format(
      template,
      userName,
      skillName,
      userName,
      "software engineer",
      chatHistory,
      userParams,
      "fostering open dialogue and reducing directive communication",
      userName
    );

    System.out.println(promptMessage);

    var messages = new ArrayList<ChatMessage>();
    messages.add(ChatMessage.userMessage(promptMessage));

    CreateChatCompletionRequest request = CreateChatCompletionRequest.newBuilder()
      .model(gptModel)
      .messages(messages)
      .responseFormat(new CreateChatCompletionRequest.ResponseFormat("text"))
//      .temperature(0.01)
      .build();

    return chatClient.createChatCompletionAsync(request)
      .thenApply(chatCompletion -> {
        if (!chatCompletion.choices().isEmpty()) {
          System.out.println("chatCompletion.choices().get(0).message().content(): " + chatCompletion.choices()
            .get(0)
            .message()
            .content());
          return new MessageDto(chatCompletion.choices().get(0).message().content());
        }

        //todo better handling for that
        return new MessageDto("empty");
      });
  }

}
