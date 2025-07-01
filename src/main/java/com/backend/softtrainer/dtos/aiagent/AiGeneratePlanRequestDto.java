package com.backend.softtrainer.dtos.aiagent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGeneratePlanRequestDto {
    private AiAgentOrganizationDto organization;
    private AiAgentSkillDto skill;
} 