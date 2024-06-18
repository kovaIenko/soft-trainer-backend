package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.utils.Converter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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

  public Optional<Chat> findChatWithMessages(final User user, final Long simulationId) {
    var chats = chatRepository.findByUserAndSimulationIdWithMessages(user, simulationId);
    var sorted = chats.stream().sorted(Comparator.comparing(Chat::getTimestamp)).toList();
    if (sorted.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(sorted.get(sorted.size() - 1));
  }

  public Optional<Chat> findChatWithMessages(final Long chatId) {
    return chatRepository.findByIdWithMessages(chatId);
  }

}
