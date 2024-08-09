package com.backend.softtrainer.dtos.innercontent;

import com.backend.softtrainer.dtos.UserHyperParamResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChartInnerContent extends InnerContentMessage {

  @JsonProperty("achieved_scores")
  private List<UserHyperParamResponseDto> values;

}
