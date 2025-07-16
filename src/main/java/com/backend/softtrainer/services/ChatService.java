package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.utils.Converter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class ChatService {

  private final ChatRepository chatRepository;
  private final EntityManager entityManager;

  /**
   * 🔄 Store chat in the same transaction context as initialization
   * Uses PROPAGATION_REQUIRED to ensure chat creation and initialization share the same transaction
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public Chat store(final Simulation simulation, final User user) {
    log.debug("💾 Storing chat for user {} and simulation {} in same transaction context", 
              user.getId(), simulation.getId());
    
    var chat = Converter.convert(simulation, user);
    
    // Save the chat in the current transaction context
    Chat savedChat = chatRepository.save(chat);
    
    log.debug("✅ Chat {} stored in current transaction context", savedChat.getId());
    return savedChat;
  }

  public boolean existsBy(final User user, final Long simulationId) {
    return chatRepository.existsByUserAndSimulationId(user, simulationId);
  }

  public Optional<Chat> findChatWithMessages(final User user, final Long simulationId) {
    var chats = chatRepository.findByUserAndSimulationNameWithMessages(user, simulationId);
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
