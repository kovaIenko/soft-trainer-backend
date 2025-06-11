package com.backend.softtrainer.dtos.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileProgressionDto {
    private String id;
    private String name;
    private List<HyperParamProgressionDto> hyperparams_progression;
} 