package com.backend.softtrainer.dtos.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamHeatmapDto {
    private List<UserHeatmapDto> users;
    private List<String> hyperParamKeys;
} 