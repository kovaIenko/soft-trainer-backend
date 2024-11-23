package com.backend.softtrainer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserHyperParamMaxValueDto(String key, Double value, @JsonProperty("max_value") Double maxValue){
}
