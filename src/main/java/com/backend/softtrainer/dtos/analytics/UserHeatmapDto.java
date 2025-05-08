package com.backend.softtrainer.dtos.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserHeatmapDto {
    private String email;
    private String name;
    private List<HyperParamRatioDto> hyperParams;
} 