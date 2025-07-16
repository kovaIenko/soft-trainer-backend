package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.aiagent.AiGeneratePlanResponseDto;
import com.backend.softtrainer.dtos.aiagent.AiSimulationDto;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.enums.SimulationComplexity;
import com.backend.softtrainer.entities.enums.SimulationType;
import com.backend.softtrainer.entities.enums.SkillGenerationStatus;
import com.backend.softtrainer.repositories.SimulationRepository;
import com.backend.softtrainer.repositories.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiPlanProcessingService {

    private final SimulationRepository simulationRepository;
    private final SkillRepository skillRepository;

    @Async("aiAgentTaskExecutor")
    @Transactional
    public CompletableFuture<Void> processAiPlanAndCreateSimulations(
            Long skillId, AiGeneratePlanResponseDto aiResponse) {

        log.info("Processing AI plan for skill ID: {} with {} simulations",
                skillId, aiResponse.getSimulations().size());

        try {
            Skill skill = skillRepository.findById(skillId)
                    .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

            // Clear existing simulations if any
            skill.getSimulations().clear();

            // Create simulations from AI response
            for (int i = 0; i < aiResponse.getSimulations().size(); i++) {
                AiSimulationDto aiSim = aiResponse.getSimulations().get(i);

                Simulation simulation = createSimulationFromAiData(aiSim, skill);
                simulationRepository.save(simulation);

                // Add to skill with order
                skill.getSimulations().put(simulation, (long) (i + 1));

                log.debug("Created simulation: {} for skill: {}", simulation.getName(), skill.getName());
            }

            // Update skill with AI metadata and mark as completed
            updateSkillWithAiMetadata(skill, aiResponse);
            skill.setGenerationStatus(SkillGenerationStatus.COMPLETED);
            skill.setHidden(false);  // Make skill visible to users now that generation is complete
            skillRepository.save(skill);

            log.info("Successfully processed AI plan for skill: {} - created {} simulations, status set to COMPLETED and made visible to users",
                    skill.getName(), aiResponse.getSimulations().size());

        } catch (Exception e) {
            log.error("Failed to process AI plan for skill ID: {}", skillId, e);

            // Update skill status to FAILED and ensure it stays hidden from users
            try {
                Skill skill = skillRepository.findById(skillId).orElse(null);
                if (skill != null) {
                    skill.setGenerationStatus(SkillGenerationStatus.FAILED);
                    skill.setHidden(true);  // Ensure failed skills remain hidden from users
                    skillRepository.save(skill);
                    log.info("Set generation status to FAILED and kept skill hidden for skill ID: {}", skillId);
                }
            } catch (Exception statusUpdateException) {
                log.error("Failed to update skill status to FAILED for skill ID: {}", skillId, statusUpdateException);
            }

            throw e;
        }

        return CompletableFuture.completedFuture(null);
    }

    private Simulation createSimulationFromAiData(AiSimulationDto aiSim, Skill skill) {
        return Simulation.builder()
                .name(aiSim.getName())
                .skill(skill)
                .type(SimulationType.AI_GENERATED)
                .complexity(mapDifficultyToComplexity(aiSim.getDifficulty()))
                .isOpen(true) // Make simulations available by default
                .hearts(0.0) // Default rating
                .nodes(Collections.emptyList())
                .build();
    }

    private SimulationComplexity mapDifficultyToComplexity(String difficulty) {
        if (difficulty == null) return SimulationComplexity.EASY;

        return switch (difficulty.toLowerCase()) {
            case "basic", "easy" -> SimulationComplexity.EASY;
            case "intermediate", "medium" -> SimulationComplexity.MEDIUM;
            case "advanced", "hard" -> SimulationComplexity.HARD;
            default -> SimulationComplexity.EASY;
        };
    }

    private void updateSkillWithAiMetadata(Skill skill, AiGeneratePlanResponseDto aiResponse) {
        // Update simulation count to match AI-generated count
        skill.setSimulationCount(aiResponse.getSimulations().size());

        // TODO: Store additional AI metadata if needed
        // For example, you could add fields to Skill entity:
        // - aiPlanSummary
        // - aiEstimatedDuration
        // - aiGenerationSuccess

        log.debug("Updated skill metadata for: {}", skill.getName());
    }

    /**
     * Scheduled task that runs every minute to check for timed-out skill generations.
     * For skills that have been in GENERATING status for more than 5 minutes:
     * - If the skill has 0 simulations: marked as FAILED and kept hidden
     * - If the skill has some simulations: marked as COMPLETED and made visible
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void handleSkillGenerationTimeouts() {
        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(5);

            // Find skills that are in GENERATING status and older than 5 minutes
            List<Skill> timedOutSkills = skillRepository.findByGenerationStatusInAndTimestampBefore(
                    List.of(SkillGenerationStatus.GENERATING),
                    timeoutThreshold
            );

            for (Skill skill : timedOutSkills) {
                // Check if the skill has 0 simulations
                if (skill.getSimulations().isEmpty()) {
                    skill.setGenerationStatus(SkillGenerationStatus.FAILED);
                    skill.setHidden(true); // Ensure failed skills remain hidden from users
                    skillRepository.save(skill);

                    log.warn("Skill generation timed out after 5 minutes for skill: {} (ID: {}). " +
                            "Status changed to FAILED and kept hidden from users.",
                            skill.getName(), skill.getId());
                } else {
                    // Skill has simulations, mark as completed and make visible
                    skill.setGenerationStatus(SkillGenerationStatus.COMPLETED);
                    skill.setHidden(false); // Make skill visible to users since it has simulations
                    skillRepository.save(skill);

                    log.info("Skill generation timed out but has {} simulations for skill: {} (ID: {}). " +
                            "Status changed to COMPLETED and made visible to users.",
                            skill.getSimulations().size(), skill.getName(), skill.getId());
                }
            }

            if (!timedOutSkills.isEmpty()) {
                int failedCount = (int) timedOutSkills.stream()
                        .filter(skill -> skill.getSimulations().isEmpty())
                        .count();
                int completedCount = timedOutSkills.size() - failedCount;

                log.info("Processed {} timed-out skills: {} marked as FAILED, {} marked as COMPLETED",
                        timedOutSkills.size(), failedCount, completedCount);
            }

        } catch (Exception e) {
            log.error("Error while handling skill generation timeouts", e);
        }
    }
}
