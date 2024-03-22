package com.backend.softtrainer.dtos;

import lombok.Data;

@Data
public class ChatRequestDto {

  private Long ownerId;

  private String flowName;

}
