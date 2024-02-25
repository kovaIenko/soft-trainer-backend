package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.utils.Converter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ChatService {

  private final ChatRepository chatRepository;

  public Chat store(final ChatRequestDto chatRequestDto) {
    var chat = Converter.convert(chatRequestDto);
    return chatRepository.save(chat);
  }

}
