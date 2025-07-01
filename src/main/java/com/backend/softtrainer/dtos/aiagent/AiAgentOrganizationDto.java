package com.backend.softtrainer.dtos.aiagent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentOrganizationDto {
    private String name;
    private String industry;
    private String size;
    private String localization;
} 