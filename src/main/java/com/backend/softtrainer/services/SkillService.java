package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.SimulationResponseDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.FlowRepository;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.SkillRepository;
import com.backend.softtrainer.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.backend.softtrainer.utils.Converter.convertSimulation;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillService {

  private final SkillRepository skillRepository;

  private final OrganizationRepository organizationRepository;

  private final UserRepository userRepository;

  private final ChatRepository chatRepository;

  private final FlowRepository flowRepository;

  private final MessageRepository messageRepository;

  public Set<Skill> getAvailableSkillByOrg(final String organization) {

    var optOrg = organizationRepository.getFirstByName(organization);
    return optOrg.map(Organization::getAvailableSkills)
      .orElse(Collections.emptySet());
  }

  public Set<Skill> getAvailableSkill(final String username) {

    var userOpt = userRepository.findByEmail(username);
    if (userOpt.isPresent()) {
      var optOrg = Optional.ofNullable(userOpt.get().getOrganization());
      return optOrg.map(Organization::getAvailableSkills)
        .orElse(Collections.emptySet());
    } else {
      var errorMessage = String.format("The user %s doesn't exist", username);
      log.error(errorMessage);
      return new HashSet<>();
    }
  }

  public Set<Skill> getAllSkill() {
    return new HashSet<>(skillRepository.findAll());
  }

  public Set<SimulationResponseDto> findSimulationsBySkill(final User user, final Long skillId) {
    var optSkill = skillRepository.findById(skillId);
    if (optSkill.isPresent()) {

      var simulations = optSkill.get().getSimulations();
      var chats = chatRepository.findAllBySkillId(user, skillId);

      var chatBySimulationId = chats.stream().collect(Collectors.groupingBy(a -> a.getSimulation().getId()));

      final AtomicBoolean isFirst = new AtomicBoolean(true);

      return simulations.entrySet().stream().sorted(java.util.Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .filter(Simulation::isOpen)
        .map(simulation -> {
          var chatsPerSimulation = chatBySimulationId.getOrDefault(simulation.getId(), List.of());

          if (chatsPerSimulation.isEmpty()) {
            if (isFirst.get()) {
              isFirst.set(false);
              return convertSimulation(simulation, true, false, simulations.get(simulation));
            } else {
              return convertSimulation(simulation, false, false, simulations.get(simulation));
            }
          }
          var atLeastOneCompleted = chatsPerSimulation.stream()
            .anyMatch(this::isChatCompleted);

          if (atLeastOneCompleted) {
            return convertSimulation(simulation, true, true, simulations.get(simulation));
          } else {
            isFirst.set(false);
            return convertSimulation(simulation, true, false, simulations.get(simulation));
          }

        })
        .sorted(Comparator.comparing(SimulationResponseDto::order))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    throw new NoSuchElementException(String.format("There is no skills with id %s", skillId));
  }

  public boolean isSimulationAvailableForUser(final User user, final Simulation simulationInput) {
    return findSimulationsBySkill(user, simulationInput.getSkill().getId()).stream().anyMatch(simulation -> simulation.id().equals(simulationInput.getId()) && simulation.available());
  }


  //todo very-very complex method
  private boolean isChatCompleted(final Chat chat) {
    var simulation = chat.getSimulation();
    var lastNode = flowRepository.findTopBySimulationOrderByOrderNumberDesc(simulation);
    if (lastNode.isEmpty()) {
      throw new RuntimeException(String.format(
        "The simulation %s of chat %s doesn't have last node",
        simulation.getId(),
        chat.getId()
      ));
    }
    var lastMessages = messageRepository.existsByOrderNumberAndChatId(chat, lastNode.get().getOrderNumber());
    return !lastMessages.isEmpty();
  }

}
