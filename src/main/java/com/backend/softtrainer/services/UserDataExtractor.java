package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.repositories.ChatRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
public class UserDataExtractor {

  private final ChatRepository chatRepository;

  private final Long ONBOARDING_SIMULATION_ID = 652L;

  public String getUserOnboardingData(final User user) {
    var onboardings = chatRepository.findByUserAndSimulationIdWithMessages(user, ONBOARDING_SIMULATION_ID);

    var onboardingChat = onboardings.stream()
      .filter(Chat::isFinished)
      .findAny()
      .orElse(onboardings.isEmpty() ? null : onboardings.get(0));

    System.out.println("Onboarding chat is " + onboardingChat.getMessages());
    if (Objects.isNull(onboardingChat)) {
      return "";
    }

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
  }

}
