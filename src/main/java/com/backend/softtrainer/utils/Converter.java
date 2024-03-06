package com.backend.softtrainer.utils;

import com.backend.softtrainer.dtos.ChatDto;
import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.dtos.MessageDto;
import com.backend.softtrainer.entities.Chat;
import lombok.NoArgsConstructor;

import java.util.stream.Collectors;

@NoArgsConstructor
public class Converter {

  public static Chat convert(final ChatRequestDto chatRequestDto) {
    return Chat.builder()
      .id(chatRequestDto.getId())
      .ownerId(chatRequestDto.getOwnerId())
      .flowName(chatRequestDto.getFlowName())
      .build();
  }

//  public static com.backend.softtrainer.interpreter.Message convert(final MessageRequestDto chatRequestDto) {
//    return com.backend.softtrainer.interpreter.Message.builder()
//      .id(chatRequestDto.getId())
//      .chatId(chatRequestDto.getChatId())
//      //.content(chatRequestDto.getContent())
//      .timestamp(chatRequestDto.getTimestamp())
//      .build();
//  }

  public static ChatDto convert(final Chat chat) {
    return new ChatDto(chat.getMessages().stream().map(a->new MessageDto("a.getContent()")).collect(Collectors.toSet()));
  }

}
