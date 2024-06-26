package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatDto;
import com.backend.softtrainer.dtos.MessageDto;
import io.github.stefanbratanov.jvm.openai.Assistant;
import io.github.stefanbratanov.jvm.openai.AssistantsClient;
import io.github.stefanbratanov.jvm.openai.ChatClient;
import io.github.stefanbratanov.jvm.openai.ChatMessage;
import io.github.stefanbratanov.jvm.openai.CreateChatCompletionRequest;
import io.github.stefanbratanov.jvm.openai.CreateRunRequest;
import io.github.stefanbratanov.jvm.openai.CreateThreadRequest;
import io.github.stefanbratanov.jvm.openai.MessagesClient;
import io.github.stefanbratanov.jvm.openai.OpenAI;
import io.github.stefanbratanov.jvm.openai.PaginationQueryParameters;
import io.github.stefanbratanov.jvm.openai.RunsClient;
import io.github.stefanbratanov.jvm.openai.ThreadMessage;
import io.github.stefanbratanov.jvm.openai.ThreadRun;
import io.github.stefanbratanov.jvm.openai.ThreadsClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatGptServiceJvmOpenAi implements ChatGptService {

  private final OpenAI openAI = OpenAI.newBuilder(System.getenv("OPEN_AI_SECRET_KEY"))
    .requestTimeout(Duration.ofSeconds(10))
    .build();

  private final ChatClient chatClient = openAI.chatClient();

  private final ThreadsClient threadsClient = openAI.threadsClient();

  private final AssistantsClient assistantsClient = openAI.assistantsClient();

  private final MessagesClient messagesClient = openAI.messagesClient();

  private final RunsClient runsClient = openAI.runsClient();

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
                                                              final String userName, final String skillName,
                                                              final String onboardingExtraction) {

    String userParams = params.entrySet().stream()
      .map(entry -> {
        var key = entry.getKey();
        var value = entry.getValue();
        return key + " = " + value;
      })
      .collect(Collectors.joining(" \n "));

    System.out.println("user params: " + userParams);

    var chatHistory = new StringBuilder();
    chat.messages().forEach(message -> ChatGptService.convert(chatHistory, message));
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

    var assistant = assistantsClient.retrieveAssistant("asst_vVoj4x1xjspaQlmxrw7IbLIE");

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


  public CompletableFuture<MessageDto> buildAfterwardSimulationRecommendation(final ChatDto chat,
                                                                              final String promptTemplate,
                                                                              final Map<String, Double> params,
                                                                              final String skillName,
                                                                              final String onboardingExtraction) throws
                                                                                                                 InterruptedException {
    String userParams = params.entrySet().stream()
      .map(entry -> {
        var key = entry.getKey();
        var value = entry.getValue();
        return key + " = " + value;
      })
      .collect(Collectors.joining(" \n "));

    System.out.println("user params: " + userParams);

    var chatHistory = new StringBuilder();
    chat.messages().forEach(message -> ChatGptService.convert(chatHistory, message));
    System.out.println("chat history: " + chatHistory);

    var promptMessage = String.format(
      promptTemplate,
      onboardingExtraction,
      chatHistory
    );

    System.out.println("The prompt with the populated values looks like " + promptMessage);

    log.info("Start of requesting LLM date {}", LocalDateTime.now());
    CreateThreadRequest createThreadRequest = CreateThreadRequest.newBuilder().build();
    var thread = threadsClient.createThread(createThreadRequest);

    Assistant assistant = assistantsClient.retrieveAssistant("asst_vVoj4x1xjspaQlmxrw7IbLIE");

    CreateRunRequest createRunRequest = CreateRunRequest.newBuilder()
      .assistantId(assistant.id())
      .instructions(promptMessage)
      .build();

    ThreadRun run = runsClient.createRun(thread.id(), createRunRequest);

    ThreadRun retrievedRun = runsClient.retrieveRun(thread.id(), run.id());
    String status = retrievedRun.status();
    System.out.println("status: " + status);
    Thread.sleep(5000);
    MessagesClient.PaginatedThreadMessages paginatedMessages = messagesClient.listMessages(
      thread.id(),
      PaginationQueryParameters.none()
    );
    List<ThreadMessage> messagesResponse = paginatedMessages.data();

    if (messagesResponse.isEmpty()) {
      log.info("End of requesting LLM date {}", LocalDateTime.now());
      log.info("we still waiting for the response");
      return CompletableFuture.completedFuture(new MessageDto(""));
    } else {

      log.info("End of requesting LLM date {}", LocalDateTime.now());
      var content = messagesResponse.get(0)
        .content()
        .stream()
        .map(cnt -> ((ThreadMessage.Content.TextContent) cnt).text().value())
        .collect(Collectors.joining(" "));

      return CompletableFuture.completedFuture(new MessageDto(content));

    }

  }

}
