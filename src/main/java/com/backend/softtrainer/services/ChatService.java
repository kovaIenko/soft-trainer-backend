package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.utils.Converter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ChatService {

  private final ChatRepository chatRepository;

  public Chat store(final ChatRequestDto chatRequestDto, final User user) {
    var chat = Converter.convert(chatRequestDto, user);
    return chatRepository.save(chat);
  }

  public boolean existsBy(final User user, final String flowName, final Long skillId) {
    return chatRepository.existsByUserAndSimulationNameAndSkillId(user, flowName, skillId);
  }

  public Optional<Chat> findChatWithMessages(final String username, final String flowName) {
    return chatRepository.findByOwnerIdAndFlowNameWithMessages(username, flowName);
  }

}
