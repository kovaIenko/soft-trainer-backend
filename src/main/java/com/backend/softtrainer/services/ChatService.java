package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.utils.Converter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class ChatService {

  private final ChatRepository chatRepository;

  public Chat store(final Simulation simulation, final User user) {
    var chat = Converter.convert(simulation, user);
    return chatRepository.save(chat);
  }

  public boolean existsBy(final User user, final Long simulationId) {
    return chatRepository.existsByUserAndSimulationId(user, simulationId);
  }

  public Optional<Chat> findChatWithMessages(final User user, final Simulation simulation) {
    return chatRepository.findByUserAndSimulationWithMessages(user, simulation);
  }

}
