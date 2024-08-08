package com.backend.softtrainer.dtos.flow;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultSimulationDto extends FlowNodeDto {

  private String prompt;

}
