package com.backend.softtrainer.dtos.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HyperParamRatioDto {
    private String key;
    private double value;
    private double maxValue;
    private double ratio;
} 