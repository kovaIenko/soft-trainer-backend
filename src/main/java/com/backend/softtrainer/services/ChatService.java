package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.entities.Chat;
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

  public Chat store(final ChatRequestDto chatRequestDto, final Long userId) {
    var chat = Converter.convert(chatRequestDto, userId);
    return chatRepository.save(chat);
  }

  public boolean existsBy(final Long ownerId, final String flowName, final Long skillId) {
    return chatRepository.existsByOwnerIdAndSimulationNameAndSkillId(ownerId, flowName, skillId);
  }

  public Optional<Chat> findChatWithMessages(final Long ownerId, final String flowName) {
    return chatRepository.findByOwnerIdAndFlowNameWithMessages(ownerId, flowName);
  }

  public List<Chat> getAll(final Long ownerId) {
    return chatRepository.findAllByOwnerId(ownerId);
  }

}
