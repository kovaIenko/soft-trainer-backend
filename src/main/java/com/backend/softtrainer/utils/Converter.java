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
      .ownerId(chatRequestDto.getOwnerId())
      .simulationName(chatRequestDto.getSimulationName())
      .skillId(chatRequestDto.getSkillId())
      .build();
  }

  public static ChatDto convert(final Chat chat) {
    return new ChatDto(chat.getMessages().stream().map(a -> new MessageDto("a.getContent()")).collect(Collectors.toSet()));
  }

}
