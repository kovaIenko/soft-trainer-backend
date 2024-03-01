package com.backend.softtrainer.dtos;

import lombok.Data;

@Data
public class ChatRequestDto {

  private String id;

  private String ownerId;

  private String flowName;

}
