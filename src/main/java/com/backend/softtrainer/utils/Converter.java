package com.backend.softtrainer.utils;

import com.backend.softtrainer.dtos.ChatDto;
import com.backend.softtrainer.dtos.MessageDto;
import com.backend.softtrainer.dtos.SimulationResponseDto;
import com.backend.softtrainer.dtos.SkillResponseDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.User;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
public class Converter {

  public static Chat convert(final Simulation simulation, final User user) {
    return Chat.builder()
      .user(user)
      .simulation(simulation)
      .build();
  }

  public static ChatDto convert(final Chat chat) {
    return new ChatDto(chat.getMessages().stream().map(a -> new MessageDto("a.getContent()")).collect(Collectors.toSet()));
  }

  public static Set<SimulationResponseDto> convertSimulations(final Set<Simulation> simulations) {
    return simulations.stream().map(node -> new SimulationResponseDto(node.getId(), node.getName())).collect(
      Collectors.toSet());
  }

  public static Set<SkillResponseDto> convertSkills(final Set<Skill> skills) {
    return skills.stream().map(skill -> new SkillResponseDto(skill.getId(), skill.getName(), skill.getAvatar())).collect(
      Collectors.toSet());
  }

}
