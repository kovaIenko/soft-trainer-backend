package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.MessageRequestDto;
import com.backend.softtrainer.entities.Message;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.utils.Converter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MessageService {

  private final MessageRepository messageRepository;

  public Message store(final MessageRequestDto messageRequestDto) {
    var messageEntity = Converter.convert(messageRequestDto);
    return messageRepository.save(messageEntity);
  }

}
