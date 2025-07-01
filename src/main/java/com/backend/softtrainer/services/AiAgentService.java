package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.aiagent.AiGeneratePlanRequestDto;
import com.backend.softtrainer.dtos.aiagent.AiGeneratePlanResponseDto;
import com.backend.softtrainer.dtos.aiagent.AiAgentOrganizationDto;
import com.backend.softtrainer.dtos.aiagent.AiAgentSkillDto;
import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.Skill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAgentService {

    private final RestTemplate restTemplate;
    
    @Value("${app.ai-agent.base-url:http://16.171.20.54:8000}")
    private String aiAgentBaseUrl;
    
    @Value("${app.ai-agent.enabled:true}")
    private boolean aiAgentEnabled;

    @Async("aiAgentTaskExecutor")
    public CompletableFuture<AiGeneratePlanResponseDto> generatePlanAsync(Skill skill, Organization organization) {
        log.info("Starting async AI plan generation for skill: {} in organization: {}", 
                skill.getName(), organization.getName());
        
        if (!aiAgentEnabled) {
            log.warn("AI Agent is disabled, skipping plan generation");
            return CompletableFuture.completedFuture(createFallbackResponse());
        }
        
        try {
            AiGeneratePlanRequestDto request = buildRequest(skill, organization);
            AiGeneratePlanResponseDto response = callAiAgent(request);
            
            log.info("Successfully generated AI plan for skill: {} with {} simulations", 
                    skill.getName(), response.getSimulations().size());
            
            return CompletableFuture.completedFuture(response);
            
        } catch (Exception e) {
            log.error("Failed to generate AI plan for skill: {}", skill.getName(), e);
            return CompletableFuture.completedFuture(createFallbackResponse());
        }
    }

    private AiGeneratePlanRequestDto buildRequest(Skill skill, Organization organization) {
        AiAgentOrganizationDto orgDto = AiAgentOrganizationDto.builder()
                .name(organization.getName())
                .industry(determineIndustry(organization))
                .size(determineOrganizationSize(organization))
                .localization("en")
                .build();

        AiAgentSkillDto skillDto = AiAgentSkillDto.builder()
                .name(skill.getName())
                .description(skill.getDescription())
                .materials(Collections.emptyList()) // TODO: Convert materials later
                .targetAudience("Employees")
                .complexityLevel("mixed")
                .expectedCountSimulations(skill.getSimulationCount() != null ? skill.getSimulationCount() : 3)
                .build();

        return AiGeneratePlanRequestDto.builder()
                .organization(orgDto)
                .skill(skillDto)
                .build();
    }

    private AiGeneratePlanResponseDto callAiAgent(AiGeneratePlanRequestDto request) {
        String url = aiAgentBaseUrl + "/generate-plan";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<AiGeneratePlanRequestDto> entity = new HttpEntity<>(request, headers);
        
        log.debug("Calling AI Agent at: {} with request: {}", url, request);
        
        ResponseEntity<AiGeneratePlanResponseDto> response = restTemplate.postForEntity(
                url, entity, AiGeneratePlanResponseDto.class);
        
        if (response.getBody() == null) {
            throw new RuntimeException("AI Agent returned null response");
        }
        
        return response.getBody();
    }

    private AiGeneratePlanResponseDto createFallbackResponse() {
        return AiGeneratePlanResponseDto.builder()
                .simulations(Collections.emptyList())
                .planSummary("AI generation failed, fallback response")
                .estimatedTotalDuration(0)
                .difficultyProgression(Collections.emptyList())
                .success(false)
                .generationMetadata(Collections.singletonMap("fallback", true))
                .build();
    }

    private String determineIndustry(Organization organization) {
        // TODO: Add industry field to Organization entity or derive from name
        return "Technology"; // Default for now
    }

    private String determineOrganizationSize(Organization organization) {
        // TODO: Add size field to Organization entity or derive from user count
        return "50-100 employees"; // Default for now
    }
} 