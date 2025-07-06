package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.MaterialMetadataDto;
import com.backend.softtrainer.dtos.NewSkillPayload;
import com.backend.softtrainer.dtos.SimulationAvailabilityStatusDto;
import com.backend.softtrainer.dtos.SimulationNodesDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.enums.SkillGenerationStatus;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.MaterialRepository;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.SkillRepository;
import com.backend.softtrainer.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  private final MaterialStorageService materialStorageService;

  private final MaterialRepository materialRepository;

  private final AiAgentService aiAgentService;

  private final AiPlanProcessingService aiPlanProcessingService;

  public Skill createSkill(NewSkillPayload payload) {
    Skill newSkill = Skill.builder()
            .name(payload.getName())
            .description(payload.getDescription())
            .type(payload.getType())
            .behavior(payload.getBehavior())
            .simulationCount(payload.getSimulationCount())
            .build();

    newSkill.setMaterials(materialStorageService.storeMaterials(payload, newSkill));

    return skillRepository.save(newSkill);
  }

  @Transactional
  public Skill createSkillForOrganization(NewSkillPayload payload, Organization organization) {

    log.info("Organization {} is creating a new skill with name: {}", organization.getName(), payload.getName());
    // Create the skill first
    Skill newSkill = Skill.builder()
            .name(payload.getName())
            .description(payload.getDescription())
            .type(payload.getType())
            .behavior(payload.getBehavior())
            .simulationCount(payload.getSimulationCount())
            .build();

    newSkill.setMaterials(materialStorageService.storeMaterials(payload, newSkill));

    // Save the skill first to get the ID
    Skill savedSkill = skillRepository.save(newSkill);

    log.info("Skill created with ID: {}", savedSkill.getId());

    // Directly insert the association without loading all skills
    organizationRepository.addSkillToOrganization(organization.getId(), savedSkill.getId());

    log.info("Created skill with ID {} and associated it with organization {}", savedSkill.getId(), organization.getName());

    // Trigger async AI plan generation
    triggerAiPlanGeneration(savedSkill, organization);

    return savedSkill;
  }

  private void triggerAiPlanGeneration(Skill skill, Organization organization) {
    log.info("Triggering AI plan generation for skill: {} in organization: {}",
             skill.getName(), organization.getName());

    try {
      // Call AI agent asynchronously
      aiAgentService.generatePlanAsync(skill, organization)
          .thenCompose(aiResponse -> {
            log.info("AI plan generation completed for skill: {}, success: {}",
                     skill.getName(), aiResponse.getSuccess());

            // Process the AI response and create simulations
            return aiPlanProcessingService.processAiPlanAndCreateSimulations(skill.getId(), aiResponse);
          })
          .exceptionally(throwable -> {
            log.error("AI plan generation failed for skill: {}", skill.getName(), throwable);

            // Update skill status to FAILED and ensure it stays hidden from users
            try {
              skill.setGenerationStatus(SkillGenerationStatus.FAILED);
              skill.setHidden(true);  // Ensure failed skills remain hidden from users
              skillRepository.save(skill);
              log.info("Set generation status to FAILED and kept skill hidden for skill: {}", skill.getName());
            } catch (Exception statusUpdateException) {
              log.error("Failed to update skill status to FAILED for skill: {}", skill.getName(), statusUpdateException);
            }

            return null;
          });

    } catch (Exception e) {
      log.error("Failed to trigger AI plan generation for skill: {}", skill.getName(), e);

      // Update skill status to FAILED and ensure it stays hidden from users
      try {
        skill.setGenerationStatus(SkillGenerationStatus.FAILED);
        skill.setHidden(true);  // Ensure failed skills remain hidden from users
        skillRepository.save(skill);
        log.info("Set generation status to FAILED and kept skill hidden for skill: {} due to trigger exception", skill.getName());
      } catch (Exception statusUpdateException) {
        log.error("Failed to update skill status to FAILED for skill: {}", skill.getName(), statusUpdateException);
      }
    }
  }

  public Skill getSkillById(Long id) {
    return skillRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Skill not found with id: " + id));
  }

  @Transactional(readOnly = true)
  public Skill getSkillByIdWithMaterials(Long id) {
    return skillRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Skill not found with id: " + id));
  }

  public List<MaterialMetadataDto> getMaterialMetadataBySkillId(Long skillId) {
    List<Object[]> results = materialRepository.findMaterialMetadataBySkillId(skillId);
    return results.stream()
            .map(row -> MaterialMetadataDto.builder()
                    .id((Long) row[0])
                    .fileName((String) row[1])
                    .tag((String) row[2])
                    .build())
            .collect(Collectors.toList());
  }

  public List<SimulationNodesDto> getSimulationsBySkillId(Long skillId) {
    Skill skill = getSkillById(skillId);
    return skill.getSimulations().entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(entry -> {
              var simulation = entry.getKey();
              // Break the recursion for DTO serialization only
              if (simulation.getNodes() != null) {
                simulation.getNodes().forEach(node -> node.setSimulation(null));
              }
              return new SimulationNodesDto(
                      simulation.getId(),
                      simulation.getName(),
                      simulation.getNodes(),
                      simulation.getAvatar(),
                      simulation.getComplexity(),
                      simulation.getCreatedAt() != null ? simulation.getCreatedAt().toString() : null,
                      skill.getId(),
                      simulation.isOpen()
              );
            })
            .collect(Collectors.toList());
  }

  public void updateSkillVisibility(Long id, boolean isHidden) {
    Skill skill = getSkillById(id);
    skill.setHidden(isHidden);
    skillRepository.save(skill);
    log.info("Updated skill visibility for skill id: {} to isHidden: {}", id, isHidden);
  }

  @Transactional
  public void deleteSkill(Long id) {
    Skill skill = getSkillById(id);
    if (skill.isProtected()) {
      throw new UnsupportedOperationException("Cannot delete a protected skill.");
    }

    // Soft delete: hide the skill from admin instead of actually deleting it
    skill.setAdminHidden(true);
    skillRepository.save(skill);

    log.info("Successfully soft deleted (admin hidden) skill with id: {}", id);
  }

  @Transactional
  public void restoreSkill(Long id) {
    Skill skill = getSkillById(id);
    skill.setAdminHidden(false);
    skillRepository.save(skill);

    log.info("Successfully restored skill with id: {}", id);
  }

  public Set<Skill> getArchivedSkills() {
    return skillRepository.findAll().stream()
        .filter(Skill::isAdminHidden)
        .collect(Collectors.toSet());
  }

  public Set<Skill> getArchivedSkillsByOrganization(String organizationName) {
    return getAvailableSkillByOrg(organizationName).stream()
        .filter(Skill::isAdminHidden)
        .collect(Collectors.toSet());
  }

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
    return skillRepository.findAll().stream()
        .filter(skill -> !skill.isAdminHidden())
        .collect(Collectors.toSet());
  }

  public Set<Skill> getSkillsByOrganization(String organizationName) {
    return getAvailableSkillByOrg(organizationName).stream()
        .filter(skill -> !skill.isAdminHidden())
        .collect(Collectors.toSet());
  }

  public Set<SimulationAvailabilityStatusDto> findSimulationsBySkill(final User user, final Long skillId) {
    var optSkill = skillRepository.findById(skillId);
    if (optSkill.isPresent()) {
      if(optSkill.get().isAdminHidden() || optSkill.get().isHidden()) {
        throw new NoSuchElementException(String.format("You have no permission to get the skill with id %s", skillId));
      }
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
