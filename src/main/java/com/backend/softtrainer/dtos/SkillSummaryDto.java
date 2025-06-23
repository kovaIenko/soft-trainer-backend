package com.backend.softtrainer.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkillSummaryDto {
    private Long id;
    private String name;
    private String description;
} 