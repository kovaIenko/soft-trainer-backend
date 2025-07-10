package com.backend.softtrainer.dtos.flow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for option objects in SingleChoiceQuestion
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionDto {
    private String id;
    private String text;
} 