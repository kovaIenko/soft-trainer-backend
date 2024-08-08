package com.backend.softtrainer.dtos.innercontent;

import com.backend.softtrainer.dtos.UserHyperParamResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
public class ChartInnerContent extends InnerContentMessage {

  @JsonProperty("achieved_scores")
  private List<UserHyperParamResponseDto> values;

}
