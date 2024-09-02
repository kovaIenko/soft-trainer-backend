package com.backend.softtrainer.dtos;

import com.backend.softtrainer.entities.enums.SimulationComplexity;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SimulationNodesDto(Long id, String name,
                                 List<FlowNode> nodes,
                                 String avatar,
                                 SimulationComplexity complexity,
                                 @JsonProperty("created_at") String createdAt,
                                 @JsonProperty("skill_id") Long skillId,
                                 @JsonProperty("is_open") boolean isOpen) {
}
