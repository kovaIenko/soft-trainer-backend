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

//  private final ChatClient client = openAI


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

    log.info("Generated Prompt for Answer Classification:\n{}", promptMessage);

    // 🟢 3. Start OpenAI API Request
    log.info("Starting OpenAI Request at {}", LocalDateTime.now());
    CreateThreadRequest createThreadRequest = CreateThreadRequest.newBuilder().build();
    var thread = threadsClient.createThread(createThreadRequest);

    Assistant assistant = assistantsClient.retrieveAssistant(prompt.getAssistantId());

    CreateRunRequest createRunRequest = CreateRunRequest.newBuilder()
      .assistantId(assistant.id())
      .instructions(promptMessage)
      .responseFormat(AssistantsResponseFormat.auto())
      .build();

    ThreadRun run = runsClient.createRun(thread.id(), Optional.empty(), createRunRequest);
    log.info("Run started with status: {}", run.status());

    // 🟢 4. Implement Proper Polling Instead of Thread.sleep()
    ThreadRun retrievedRun;
    do {
      Thread.sleep(1000); // Poll every second
      retrievedRun = runsClient.retrieveRun(thread.id(), run.id());
      log.info("Polling OpenAI API, status: {}", retrievedRun.status());
    } while (!retrievedRun.status().equals("completed") && !retrievedRun.status().equals("failed"));

    // 🟢 5. Handle OpenAI API Response
    if (!retrievedRun.status().equals("completed")) {
      log.warn("OpenAI API request failed or did not return a valid response.");
      return CompletableFuture.completedFuture(new MessageDto("AI classification failed."));
    }

    MessagesClient.PaginatedThreadMessages paginatedMessages = messagesClient.listMessages(
      thread.id(),
      PaginationQueryParameters.none(),
      Optional.empty()
    );
    List<ThreadMessage> messagesResponse = paginatedMessages.data();

    if (messagesResponse.isEmpty()) {
      log.warn("OpenAI returned an empty response.");
      return CompletableFuture.completedFuture(new MessageDto(""));
    }

    String content = messagesResponse.get(0)
      .content()
      .stream()
      .map(cnt -> ((ThreadMessage.Content.TextContent) cnt).text().value())
      .collect(Collectors.joining(" "));

    log.info("AI Classification successfully completed.");
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

    // 🟢 1. Collect and Format User Parameters
    String userParams = params.entrySet().stream()
      .map(entry -> String.format("%s = %.2f", entry.getKey(), entry.getValue())) // Format values properly
      .collect(Collectors.joining("\n"));

    log.info("User Parameters:\n{}", userParams);

    // 🟢 2. Collect and Format Chat History
    var chatHistory = new StringBuilder();
    chat.messages()
      .stream()
      .sorted(Comparator.comparing(Message::getTimestamp))
      .forEach(message -> ChatGptService.convert(chatHistory, message));

    log.info("Chat History:\n{}", chatHistory);

    // 🟢 3. Construct Structured Prompt
    String promptMessage = String.format(
      prompt.getPrompt(),
      onboardingExtraction,
      chatHistory,
      localization
    );

    log.info("Generated Prompt for Simulation Summary:\n{}", promptMessage);

    // 🟢 4. Start OpenAI API Request
    log.info("Starting OpenAI Request at {}", LocalDateTime.now());
    CreateThreadRequest createThreadRequest = CreateThreadRequest.newBuilder().build();
    var thread = threadsClient.createThread(createThreadRequest);

    Assistant assistant = assistantsClient.retrieveAssistant(prompt.getAssistantId());

    CreateRunRequest createRunRequest = CreateRunRequest.newBuilder()
      .assistantId(assistant.id())
      .instructions(promptMessage)
      .responseFormat(AssistantsResponseFormat.auto())
      .build();

    ThreadRun run = runsClient.createRun(thread.id(), Optional.empty(), createRunRequest);
    log.info("Run started with status: {}", run.status());

    // 🟢 5. Implement Proper Polling Instead of Thread.sleep()
    ThreadRun retrievedRun;
    do {
      Thread.sleep(1000); // Poll every second
      retrievedRun = runsClient.retrieveRun(thread.id(), run.id());
      log.info("Polling OpenAI API, status: {}", retrievedRun.status());
    } while (!retrievedRun.status().equals("completed") && !retrievedRun.status().equals("failed"));

    // 🟢 6. Handle OpenAI API Response
    if (!retrievedRun.status().equals("completed")) {
      log.warn("OpenAI API request failed or did not return a valid response.");
      return CompletableFuture.completedFuture(new MessageDto("AI simulation summary generation failed."));
    }

    MessagesClient.PaginatedThreadMessages paginatedMessages = messagesClient.listMessages(
      thread.id(),
      PaginationQueryParameters.none(),
      Optional.empty()
    );
    List<ThreadMessage> messagesResponse = paginatedMessages.data();

    if (messagesResponse.isEmpty()) {
      log.warn("OpenAI returned an empty response.");
      return CompletableFuture.completedFuture(new MessageDto(""));
    }

    String content = messagesResponse.get(0)
      .content()
      .stream()
      .map(cnt -> ((ThreadMessage.Content.TextContent) cnt).text().value())
      .collect(Collectors.joining(" "));

    log.info("AI Simulation Summary successfully generated.");
    return CompletableFuture.completedFuture(new MessageDto(content));
  }


  @Override
  public CompletableFuture<MessageDto> buildAfterwardActionableHintMessage(final ChatDto chat,
                                                                           final List<Message> actionableMessages,
                                                                           final Prompt prompt,
                                                                           final Map<String, Double> params,
                                                                           final String skillName,
                                                                           final String onboardingExtraction,
                                                                           final String localization) throws
                                                                                                      InterruptedException {


    var lastActionableMessage = new StringBuilder();


    log.info("actionableMessages {}", chat.messages());
    log.info("chat {}", actionableMessages);

    log.info("No actionable messages found, using last 6 chat messages.");
    List<Message> lastThreeMessages = chat.messages().stream()
      .sorted(Comparator.comparing(Message::getTimestamp))
//      .skip(Math.max(0, chat.messages().size() - 6))
      .toList();
    lastThreeMessages.forEach(msg -> ChatGptService.convert(lastActionableMessage, msg));

    log.info("chat {}", lastActionableMessage);

    var promptMessage = String.format(
      prompt.getPrompt(),
      onboardingExtraction,
      lastActionableMessage,
      localization
    );

    log.info("The prompt with the populated values for hint message looks like {}", promptMessage);

    log.info("Start OpenAI request at {}", LocalDateTime.now());

    CreateThreadRequest createThreadRequest = CreateThreadRequest.newBuilder().build();
    var thread = threadsClient.createThread(createThreadRequest);

    Assistant assistant = assistantsClient.retrieveAssistant(prompt.getAssistantId());

    CreateRunRequest createRunRequest = CreateRunRequest.newBuilder()
      .assistantId(assistant.id())
      .instructions(promptMessage)
      .responseFormat(AssistantsResponseFormat.auto())
      .build();

    ThreadRun run = runsClient.createRun(thread.id(), Optional.empty(), createRunRequest);
    log.info("Run started with status: {}", run.status());

// Polling mechanism for waiting on OpenAI response
    ThreadRun retrievedRun;
    do {
      Thread.sleep(1000);
      retrievedRun = runsClient.retrieveRun(thread.id(), run.id());
      log.info("Polling OpenAI API, status: {}", retrievedRun.status());
    } while (!retrievedRun.status().equals("completed") && !retrievedRun.status().equals("failed"));

    if (!retrievedRun.status().equals("completed")) {
      log.warn("OpenAI API did not return a valid response.");
      return CompletableFuture.completedFuture(new MessageDto("AI hint generation failed."));
    }

    MessagesClient.PaginatedThreadMessages paginatedMessages = messagesClient.listMessages(
      thread.id(),
      PaginationQueryParameters.none(),
      Optional.empty()
    );
    List<ThreadMessage> messagesResponse = paginatedMessages.data();

    if (messagesResponse.isEmpty()) {
      log.warn("OpenAI returned an empty response.");
      return CompletableFuture.completedFuture(new MessageDto(""));
    }

    String content = messagesResponse.get(0)
      .content()
      .stream()
      .map(cnt -> ((ThreadMessage.Content.TextContent) cnt).text().value())
      .collect(Collectors.joining(" "));

    log.info("AI hint generated successfully.");
    return CompletableFuture.completedFuture(new MessageDto(content));
  }

  @Override
  public String generateOverview(String prompt, String model) {
    // TODO: Replace with real OpenAI call
    log.info("[MOCK] Sending prompt to LLM ({}): {}", model, prompt);
    // For now, return a mock response
    return "[AI Overview] This is a mock summary based on the provided analytics.";
  }

}
