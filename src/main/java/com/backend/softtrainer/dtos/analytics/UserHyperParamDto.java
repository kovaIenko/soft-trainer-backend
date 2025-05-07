package com.backend.softtrainer.dtos.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserHyperParamDto {
    private String key;
    private Double value;
    private Double maxValue;
} 