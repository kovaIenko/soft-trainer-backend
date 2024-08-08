package com.backend.softtrainer.dtos.flow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImagesDto extends FlowNodeDto {

  private String url;

}
