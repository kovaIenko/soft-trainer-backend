package com.backend.softtrainer.utils;

import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.dtos.MessageRequestDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Message;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Converter {

  public static Chat convert(final ChatRequestDto chatRequestDto) {
    return Chat.builder()
      .id(chatRequestDto.getId())
      .ownerId(chatRequestDto.getOwnerId())
      .build();
  }

  public static Message convert(final MessageRequestDto chatRequestDto) {
    return Message.builder()
      .id(chatRequestDto.getId())
      .chatId(chatRequestDto.getChatId())
      .content(chatRequestDto.getContent())
      .timestamp(chatRequestDto.getTimestamp())
      .build();
  }

}
