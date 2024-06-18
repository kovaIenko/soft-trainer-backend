package com.backend.softtrainer.services;

import org.springframework.stereotype.Component;

@Component
public class PromptService {

  public void validateSimulationSummary(String answer, String userName) {
    if (answer.length() > 150) {
      throw new IllegalArgumentException("Incorrect answer from chat gpt");
    }
  }
}
