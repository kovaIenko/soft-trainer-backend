package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.SimulationAvailabilityStatusDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.SkillRepository;
import com.backend.softtrainer.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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

  public Set<Skill> getAvailableSkillByOrg(final String organization) {

    var optOrg = organizationRepository.getFirstByName(organization);
    return getAvailableSkillsByOrgName(optOrg);
  }

  private @NotNull Set<Skill> getAvailableSkillsByOrgName(final Optional<Organization> optOrg) {
    //todo for now like that due to the restriction 1 user <-> 1 org
//    var optOnboardingOrg = organizationRepository.getFirstByName("Onboarding");

    return optOrg.map(Organization::getAvailableSkills)
      .map(skills -> {
//        optOnboardingOrg.ifPresent(value -> skills.addAll(value.getAvailableSkills()));
        return skills;
      })
      .orElse(Collections.emptySet());
  }

  public Set<Skill> getAvailableSkill(final String username) {

    var userOpt = userRepository.findByEmail(username);
    if (userOpt.isPresent()) {
      var optOrg = Optional.ofNullable(userOpt.get().getOrganization());
      return getAvailableSkillsByOrgName(optOrg);
    } else {
      log.error("The user {} doesn't exist", username);
      return new HashSet<>();
    }
  }

  public Set<Skill> getAllSkill() {
    return new HashSet<>(skillRepository.findAll());
  }

  public Set<SimulationAvailabilityStatusDto> findSimulationsBySkill(final User user, final Long skillId) {
    var optSkill = skillRepository.findById(skillId);
    if (optSkill.isPresent()) {
      log.info("Skill {} is present ", skillId);
      var simulations = optSkill.get().getSimulations();
      var chats = chatRepository.findAllBySkillId(user, skillId);
      var chatBySimulationId = chats.stream().collect(Collectors.groupingBy(a -> a.getSimulation().getId()));
      final AtomicBoolean isFirst = new AtomicBoolean(true);

      return simulations.entrySet().stream().sorted(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .filter(Simulation::isOpen)
        .map(simulation -> {
          var chatsPerSimulation = chatBySimulationId.getOrDefault(simulation.getId(), List.of());

          log.info(
            "For simulation {} with the order {} there are {} chats",
            simulation.getId(),
            simulations.get(simulation),
            chatsPerSimulation.size()
          );

          if (chatsPerSimulation.isEmpty()) {
            if (isFirst.get()) {
              isFirst.set(false);
              return convertSimulation(simulation, true, false, simulations.get(simulation));
            } else {
              return convertSimulation(simulation, false, false, simulations.get(simulation));
            }
          }

          var atLeastOneCompleted = chatsPerSimulation.stream()
            .anyMatch(Chat::isFinished);

          log.info(
            "At least one completed chat: {} for simulation {}",
            atLeastOneCompleted,
            simulation.getId()
          );

          if (atLeastOneCompleted) {
            return convertSimulation(simulation, true, true, simulations.get(simulation));
          } else {
            isFirst.set(false);
            return convertSimulation(simulation, true, false, simulations.get(simulation));
          }

        })
        .sorted(Comparator.comparing(SimulationAvailabilityStatusDto::order))
        .peek(simulation -> log.info("Simulation {} is available {}", simulation.id(), simulation.available()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    throw new NoSuchElementException(String.format("There is no skills with id %s", skillId));
  }

}
