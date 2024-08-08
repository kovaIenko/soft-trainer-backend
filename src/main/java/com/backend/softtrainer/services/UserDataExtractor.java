package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.messages.EnterTextAnswerMessage;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.repositories.ChatRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class UserDataExtractor {

  private final ChatRepository chatRepository;

  private final String ONBOARDING_SIMULATION_NAME = "Onboarding";

  public String getUserOnboardingData(final User user) {
    try {

      var onboardingChatOpt = getFirstChatOfOnboarding(user);
      if (onboardingChatOpt.isEmpty()) {
        return "";
      }

      var onboardingChat = onboardingChatOpt.get();
      log.info("Onboarding chat with messages: {}", onboardingChat.getMessages());

      var stringBuilder = new StringBuilder();

      onboardingChat.getMessages()
        .stream()
        .filter(msg -> !msg.getMessageType().equals(MessageType.RESULT_SIMULATION))
        .forEach(msg -> {
          ChatGptService.convert(stringBuilder, msg);
          stringBuilder.append("\n");
        });

      var onboardingExtraction = stringBuilder.toString();
      log.info("Onboarding extraction looks like {}", onboardingExtraction);
      return onboardingExtraction;
    } catch (Exception e) {
      log.error("Error while extracting user onboarding data", e);
      return "";
    }
  }

  public Optional<Chat> getFirstChatOfOnboarding(final User user) {
    var onboardings = chatRepository.findByUserAndSimulationNameWithMessages(user, ONBOARDING_SIMULATION_NAME);
    return onboardings.stream()
      .filter(Chat::isFinished)
      .findFirst();
  }

  public Optional<String> extractUserName(final User user) {
   var chatOpt = getFirstChatOfOnboarding(user);
    return chatOpt.flatMap(chat -> chat.getMessages().stream()
      .sorted(Comparator.comparing(Message::getTimestamp))
      .filter(msg -> msg instanceof EnterTextAnswerMessage)
      .map(msg -> (EnterTextAnswerMessage) msg)
      .map(EnterTextAnswerMessage::getContent)
      .findFirst());
  }

}
