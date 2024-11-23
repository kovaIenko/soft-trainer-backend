package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HyperParameterDto(String key, String description, @JsonProperty("max_value") Double maxValue) {
}
