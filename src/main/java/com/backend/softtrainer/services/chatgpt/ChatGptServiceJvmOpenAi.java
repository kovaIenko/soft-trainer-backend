package com.backend.softtrainer.services.chatgpt;

import com.backend.softtrainer.dtos.ChatDto;
import com.backend.softtrainer.dtos.MessageDto;
import com.backend.softtrainer.entities.Prompt;
import com.backend.softtrainer.entities.messages.EnterTextQuestionMessage;
import com.backend.softtrainer.entities.messages.Message;
import io.github.stefanbratanov.jvm.openai.Assistant;
import io.github.stefanbratanov.jvm.openai.AssistantsClient;
import io.github.stefanbratanov.jvm.openai.AssistantsResponseFormat;
import io.github.stefanbratanov.jvm.openai.ChatClient;
import io.github.stefanbratanov.jvm.openai.CreateRunRequest;
import io.github.stefanbratanov.jvm.openai.CreateThreadRequest;
import io.github.stefanbratanov.jvm.openai.MessagesClient;
import io.github.stefanbratanov.jvm.openai.OpenAI;
import io.github.stefanbratanov.jvm.openai.PaginationQueryParameters;
import io.github.stefanbratanov.jvm.openai.ResponseFormat;
import io.github.stefanbratanov.jvm.openai.RunsClient;
import io.github.stefanbratanov.jvm.openai.ThreadMessage;
import io.github.stefanbratanov.jvm.openai.ThreadRun;
import io.github.stefanbratanov.jvm.openai.ThreadsClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private final String gptModel = "gpt-4o-mini";

  // Add logging for OpenAI client initialization
  {
    log.info("[OpenAI] Initializing OpenAI client with timeout: {} seconds", 10);
    log.info("[OpenAI] Using model: {}", gptModel);
  }

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

//  public CompletableFuture<MessageDto> buildSimulationSummary(final ChatDto chat, String template, Map<String, Double> params,
//                                                              final String userName, final String skillName,
//                                                              final String onboardingExtraction) {
//
//    String userParams = params.entrySet().stream()
//      .map(entry -> {
//        var key = entry.getKey();
//        var value = entry.getValue();
//        return key + " = " + value;
//      })
//      .collect(Collectors.joining(" \n "));
//
//    System.out.println("user params: " + userParams);
//
//    var chatHistory = new StringBuilder();
//    chat.messages().forEach(message -> ChatGptService.convert(chatHistory, message));
//    System.out.println("chat history: " + chatHistory);
//
//    var promptMessage = String.format(
//      template,
//      userName,
//      skillName,
//      userName,
//      "software engineer",
//      chatHistory,
//      userParams,
//      "fostering open dialogue and reducing directive communication",
//      userName
//    );
//
//    log.info(promptMessage);
//
//    var messages = new ArrayList<ChatMessage>();
//    messages.add(ChatMessage.userMessage(promptMessage));
//
//
////    CreateChatCompletionRequest request = CreateChatCompletionRequest.newBuilder()
////      .model(gptModel)
////      .messages(messages)
////      .responseFormat(new CreateChatCompletionRequest.ResponseFormat("text"))
//////      .temperature(0.01)
////      .build();
//
//    var assistant = assistantsClient.retrieveAssistant("asst_vVoj4x1xjspaQlmxrw7IbLIE");
//
//    return chatClient.createChatCompletionAsync(request)
//      .thenApply(chatCompletion -> {
//        if (!chatCompletion.choices().isEmpty()) {
//          System.out.println("chatCompletion.choices().get(0).message().content(): " + chatCompletion.choices()
//            .get(0)
//            .message()
//            .content());
//          return new MessageDto(chatCompletion.choices().get(0).message().content());
//        }
//
//        //todo better handling for that
//        return new MessageDto("empty");
//      });
//  }


  @Override
  public CompletableFuture<MessageDto> classifyUserAnswer(
    final EnterTextQuestionMessage message,
    final Prompt prompt
  ) throws InterruptedException {
    log.info("[Classification] Starting answer classification at {}", LocalDateTime.now());
    log.info("[Classification] Using assistant ID: {}", prompt.getAssistantId());
    log.info("[Classification] Message options: {}", message.getOptions());
    log.info("[Classification] User answer length: {} characters", message.getOpenAnswer().length());

    // 🟢 1. Collect and Format Options for Classification
    String optionsFormatted = Arrays.stream(message.getOptions().split("\\|\\|"))
      .map(String::trim)
      .map(option -> String.format("- %s", "\"" + option + "\""))
      .collect(Collectors.joining("\n"));

    // 🟢 2. Construct Structured Prompt
    String promptMessage = String.format(
      prompt.getPrompt(),
      message.getOpenAnswer(),
      optionsFormatted
    );

    log.info("[Classification] Generated prompt (length: {} characters)", promptMessage.length());
    log.debug("[Classification] Full prompt:\n{}", promptMessage);

    // 🟢 3. Start OpenAI API Request
    log.info("[Classification] Creating new thread...");
    CreateThreadRequest createThreadRequest = CreateThreadRequest.newBuilder().build();
    var thread = threadsClient.createThread(createThreadRequest);
    log.info("[Classification] Thread created with ID: {}", thread.id());

    log.info("[Classification] Retrieving assistant...");
//    Assistant assistant = assistantsClient.retrieveAssistant(prompt.getAssistantId());
//    log.info("[Classification] Retrieved assistant: {} (ID: {})", assistant.name(), assistant.id());

    CreateRunRequest createRunRequest = CreateRunRequest.newBuilder()
      .assistantId(prompt.getAssistantId())
      .instructions(promptMessage)
      .responseFormat(AssistantsResponseFormat.responseFormat(ResponseFormat.json()))
      .build();

    ThreadRun run = runsClient.createRun(thread.id(), Optional.empty(), createRunRequest);
    log.info("[Classification] Run created with ID: {} and initial status: {}", run.id(), run.status());

    // 🟢 4. Implement Proper Polling Instead of Thread.sleep()
    ThreadRun retrievedRun;
    int pollCount = 0;
    LocalDateTime startTime = LocalDateTime.now();
    do {
      Thread.sleep(1000); // Poll every second
      pollCount++;
      retrievedRun = runsClient.retrieveRun(thread.id(), run.id());
      log.info("[Classification] Poll #{}, status: {}, elapsed time: {} seconds",
        pollCount,
        retrievedRun.status(),
        Duration.between(startTime, LocalDateTime.now()).getSeconds()
      );
    } while (!retrievedRun.status().equals("completed") && !retrievedRun.status().equals("failed"));

    // 🟢 5. Handle OpenAI API Response
    if (!retrievedRun.status().equals("completed")) {
      log.warn("[Classification] Run failed with status: {}", retrievedRun.status());
      if (retrievedRun.lastError() != null) {
        log.error("[Classification] Run error: {}", retrievedRun.lastError().message());
      }
      return CompletableFuture.completedFuture(new MessageDto("AI classification failed."));
    }

    log.info("[Classification] Run completed successfully, retrieving messages...");
    MessagesClient.PaginatedThreadMessages paginatedMessages = messagesClient.listMessages(
      thread.id(),
      PaginationQueryParameters.none(),
      Optional.empty()
    );
    List<ThreadMessage> messagesResponse = paginatedMessages.data();

    if (messagesResponse.isEmpty()) {
      log.warn("[Classification] No messages found in thread");
      return CompletableFuture.completedFuture(new MessageDto(""));
    }

    String content = messagesResponse.get(0)
      .content()
      .stream()
      .map(cnt -> ((ThreadMessage.Content.TextContent) cnt).text().value())
      .collect(Collectors.joining(" "));

    log.info("[Classification] Successfully generated classification (length: {} characters)", content.length());
    log.debug("[Classification] Classification content: {}", content);
    return CompletableFuture.completedFuture(new MessageDto(content));
  }


  @Override
  public CompletableFuture<MessageDto> buildAfterwardSimulationRecommendation(
    final ChatDto chat,
    final Prompt prompt,
    final Map<String, Double> params,
    final String skillName,
    final String onboardingExtraction,
    final String localization
  ) throws InterruptedException {
    log.info("[Simulation] Starting simulation recommendation at {}", LocalDateTime.now());
    log.info("[Simulation] Using assistant ID: {}", prompt.getAssistantId());
    log.info("[Simulation] Skill name: {}", skillName);
    log.info("[Simulation] Localization: {}", localization);
    log.info("[Simulation] Number of parameters: {}", params.size());
    log.info("[Simulation] Onboarding extraction length: {} characters", onboardingExtraction.length());

    // 🟢 1. Collect and Format User Parameters
    String userParams = params.entrySet().stream()
      .map(entry -> String.format("%s = %.2f", entry.getKey(), entry.getValue()))
      .collect(Collectors.joining("\n"));

    log.info("[Simulation] Formatted user parameters:\n{}", userParams);

    // 🟢 2. Collect and Format Chat History
    var chatHistory = new StringBuilder();
    chat.messages()
      .stream()
      .sorted(Comparator.comparing(Message::getTimestamp))
      .forEach(message -> ChatGptService.convert(chatHistory, message));

    log.info("[Simulation] Chat history length: {} characters", chatHistory.length());
    log.debug("[Simulation] Full chat history:\n{}", chatHistory);

    // 🟢 3. Construct Structured Prompt
    String promptMessage = String.format(
      prompt.getPrompt(),
      onboardingExtraction,
      chatHistory,
      localization
    );

    log.info("[Simulation] Generated prompt (length: {} characters)", promptMessage.length());
    log.debug("[Simulation] Full prompt:\n{}", promptMessage);

    // 🟢 4. Start OpenAI API Request
    log.info("[Simulation] Creating new thread...");
    CreateThreadRequest createThreadRequest = CreateThreadRequest.newBuilder().build();
    var thread = threadsClient.createThread(createThreadRequest);
    log.info("[Simulation] Thread created with ID: {}", thread.id());

    log.info("[Simulation] Retrieving assistant...");
    Assistant assistant = assistantsClient.retrieveAssistant(prompt.getAssistantId());
    log.info("[Simulation] Retrieved assistant: {} (ID: {})", assistant.name(), assistant.id());

    CreateRunRequest createRunRequest = CreateRunRequest.newBuilder()
      .assistantId(assistant.id())
      .instructions(promptMessage)
      .responseFormat(AssistantsResponseFormat.auto())
      .build();

    ThreadRun run = runsClient.createRun(thread.id(), Optional.empty(), createRunRequest);
    log.info("[Simulation] Run created with ID: {} and initial status: {}", run.id(), run.status());

    // 🟢 5. Implement Proper Polling Instead of Thread.sleep()
    ThreadRun retrievedRun;
    int pollCount = 0;
    LocalDateTime startTime = LocalDateTime.now();
    do {
      Thread.sleep(1000); // Poll every second
      pollCount++;
      retrievedRun = runsClient.retrieveRun(thread.id(), run.id());
      log.info("[Simulation] Poll #{}, status: {}, elapsed time: {} seconds",
        pollCount,
        retrievedRun.status(),
        Duration.between(startTime, LocalDateTime.now()).getSeconds()
      );
    } while (!retrievedRun.status().equals("completed") && !retrievedRun.status().equals("failed"));

    // 🟢 6. Handle OpenAI API Response
    if (!retrievedRun.status().equals("completed")) {
      log.warn("[Simulation] Run failed with status: {}", retrievedRun.status());
      if (retrievedRun.lastError() != null) {
        log.error("[Simulation] Run error: {}", retrievedRun.lastError().message());
      }
      return CompletableFuture.completedFuture(new MessageDto("AI simulation summary generation failed."));
    }

    log.info("[Simulation] Run completed successfully, retrieving messages...");
    MessagesClient.PaginatedThreadMessages paginatedMessages = messagesClient.listMessages(
      thread.id(),
      PaginationQueryParameters.none(),
      Optional.empty()
    );
    List<ThreadMessage> messagesResponse = paginatedMessages.data();

    if (messagesResponse.isEmpty()) {
      log.warn("[Simulation] No messages found in thread");
      return CompletableFuture.completedFuture(new MessageDto(""));
    }

    String content = messagesResponse.get(0)
      .content()
      .stream()
      .map(cnt -> ((ThreadMessage.Content.TextContent) cnt).text().value())
      .collect(Collectors.joining(" "));

    log.info("[Simulation] Successfully generated recommendation (length: {} characters)", content.length());
    log.debug("[Simulation] Recommendation content: {}", content);
    return CompletableFuture.completedFuture(new MessageDto(content));
  }


  @Override
  public CompletableFuture<MessageDto> buildAfterwardActionableHintMessage(
    final ChatDto chat,
    final List<Message> actionableMessages,
    final Prompt prompt,
    final Map<String, Double> params,
    final String skillName,
    final String onboardingExtraction,
    final String localization
  ) throws InterruptedException {
    log.info("[Hint] Starting hint message generation at {}", LocalDateTime.now());
    log.info("[Hint] Using assistant ID: {}", prompt.getAssistantId());
    log.info("[Hint] Skill name: {}", skillName);
    log.info("[Hint] Localization: {}", localization);
    log.info("[Hint] Number of actionable messages: {}", actionableMessages.size());
    log.info("[Hint] Onboarding extraction length: {} characters", onboardingExtraction.length());

    var lastActionableMessage = new StringBuilder();

    log.info("[Hint] Processing chat messages: {}", chat.messages().size());
    log.info("[Hint] Processing actionable messages: {}", actionableMessages.size());

    log.info("[Hint] Using last messages from chat");
    List<Message> lastThreeMessages = chat.messages().stream()
      .sorted(Comparator.comparing(Message::getTimestamp))
      .toList();
    lastThreeMessages.forEach(msg -> ChatGptService.convert(lastActionableMessage, msg));

    log.info("[Hint] Formatted messages length: {} characters", lastActionableMessage.length());
    log.debug("[Hint] Formatted messages:\n{}", lastActionableMessage);

    var promptMessage = String.format(
      prompt.getPrompt(),
      onboardingExtraction,
      lastActionableMessage,
      localization
    );

    log.info("[Hint] Generated prompt (length: {} characters)", promptMessage.length());
    log.debug("[Hint] Full prompt:\n{}", promptMessage);

    log.info("[Hint] Creating new thread...");
    CreateThreadRequest createThreadRequest = CreateThreadRequest.newBuilder().build();
    var thread = threadsClient.createThread(createThreadRequest);
    log.info("[Hint] Thread created with ID: {}", thread.id());

    log.info("[Hint] Retrieving assistant...");
    Assistant assistant = assistantsClient.retrieveAssistant(prompt.getAssistantId());
    log.info("[Hint] Retrieved assistant: {} (ID: {})", assistant.name(), assistant.id());

    CreateRunRequest createRunRequest = CreateRunRequest.newBuilder()
      .assistantId(assistant.id())
      .instructions(promptMessage)
      .responseFormat(AssistantsResponseFormat.auto())
      .build();

    ThreadRun run = runsClient.createRun(thread.id(), Optional.empty(), createRunRequest);
    log.info("[Hint] Run created with ID: {} and initial status: {}", run.id(), run.status());

    // Polling mechanism for waiting on OpenAI response
    ThreadRun retrievedRun;
    int pollCount = 0;
    LocalDateTime startTime = LocalDateTime.now();
    do {
      Thread.sleep(1000);
      pollCount++;
      retrievedRun = runsClient.retrieveRun(thread.id(), run.id());
      log.info("[Hint] Poll #{}, status: {}, elapsed time: {} seconds",
        pollCount,
        retrievedRun.status(),
        Duration.between(startTime, LocalDateTime.now()).getSeconds()
      );
    } while (!retrievedRun.status().equals("completed") && !retrievedRun.status().equals("failed"));

    if (!retrievedRun.status().equals("completed")) {
      log.warn("[Hint] Run failed with status: {}", retrievedRun.status());
      if (retrievedRun.lastError() != null) {
        log.error("[Hint] Run error: {}", retrievedRun.lastError().message());
      }
      return CompletableFuture.completedFuture(new MessageDto("AI hint generation failed."));
    }

    log.info("[Hint] Run completed successfully, retrieving messages...");
    MessagesClient.PaginatedThreadMessages paginatedMessages = messagesClient.listMessages(
      thread.id(),
      PaginationQueryParameters.none(),
      Optional.empty()
    );
    List<ThreadMessage> messagesResponse = paginatedMessages.data();

    if (messagesResponse.isEmpty()) {
      log.warn("[Hint] No messages found in thread");
      return CompletableFuture.completedFuture(new MessageDto(""));
    }

    String content = messagesResponse.get(0)
      .content()
      .stream()
      .map(cnt -> ((ThreadMessage.Content.TextContent) cnt).text().value())
      .collect(Collectors.joining(" "));

    log.info("[Hint] Successfully generated hint (length: {} characters)", content.length());
    log.debug("[Hint] Hint content: {}", content);
    return CompletableFuture.completedFuture(new MessageDto(content));
  }

  @Override
  public String generateOverview(String prompt, String assistantId, String model) {
    log.info("[AI Overview] Starting overview generation at {}", LocalDateTime.now());
    log.info("[AI Overview] Using assistant ID: {}", assistantId);
    log.info("[AI Overview] Using model: {}", model);
    log.info("[AI Overview] Prompt length: {} characters", prompt.length());

    try {
      // Create a new thread
      log.info("[AI Overview] Creating new thread...");
      CreateThreadRequest createThreadRequest = CreateThreadRequest.newBuilder().build();
      var thread = threadsClient.createThread(createThreadRequest);
      log.info("[AI Overview] Thread created with ID: {}", thread.id());

      log.info("[AI Overview] Retrieving assistant...");
//      Assistant assistant = assistantsClient.retrieveAssistant(assistantId);
//      log.info("[AI Overview] Retrieved assistant: {} (ID: {})", assistant.name(), assistant.id());

      // Create and start a run
      log.info("[AI Overview] Creating run with instructions...");
      CreateRunRequest createRunRequest = CreateRunRequest.newBuilder()
        .assistantId(assistantId)
        .instructions(prompt)
        .responseFormat(AssistantsResponseFormat.responseFormat(ResponseFormat.json()))
        .build();

      ThreadRun run = runsClient.createRun(thread.id(), Optional.empty(), createRunRequest);
      log.info("[AI Overview] Run created with ID: {} and initial status: {}", run.id(), run.status());

      // Poll for completion
      ThreadRun retrievedRun;
      int pollCount = 0;
      LocalDateTime startTime = LocalDateTime.now();
      do {
        Thread.sleep(1000); // Poll every second
        pollCount++;
        retrievedRun = runsClient.retrieveRun(thread.id(), run.id());
        log.info("[AI Overview] Poll #{}, status: {}, elapsed time: {} seconds",
          pollCount,
          retrievedRun.status(),
          Duration.between(startTime, LocalDateTime.now()).getSeconds()
        );
      } while (!retrievedRun.status().equals("completed") && !retrievedRun.status().equals("failed"));

      if (!retrievedRun.status().equals("completed")) {
        log.warn("[AI Overview] Run failed with status: {}", retrievedRun.status());
        if (retrievedRun.lastError() != null) {
          log.error("[AI Overview] Run error: {}", retrievedRun.lastError().message());
        }
        return "Failed to generate AI overview. Please try again later.";
      }

      // Get the response
      log.info("[AI Overview] Run completed successfully, retrieving messages...");
      MessagesClient.PaginatedThreadMessages paginatedMessages = messagesClient.listMessages(
        thread.id(),
        PaginationQueryParameters.none(),
        Optional.empty()
      );
      List<ThreadMessage> messagesResponse = paginatedMessages.data();

      if (messagesResponse.isEmpty()) {
        log.warn("[AI Overview] No messages found in thread");
        return "No overview could be generated at this time.";
      }

      String content = messagesResponse.get(0)
        .content()
        .stream()
        .map(cnt -> ((ThreadMessage.Content.TextContent) cnt).text().value())
        .collect(Collectors.joining(" "));

      log.info("[AI Overview] Successfully generated overview (length: {} characters)", content.length());
      log.debug("[AI Overview] Overview content: {}", content);
      return content;

    } catch (Exception e) {
      log.error("[AI Overview] Error generating overview", e);
      return "Error generating AI overview: " + e.getMessage();
    }
  }

}
