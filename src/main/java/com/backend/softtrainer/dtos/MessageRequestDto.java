package com.backend.softtrainer.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageRequestDto {

  private String id;

  private String ownerId;

  private LocalDateTime timestamp;

  private String content;

  private String chatId;

}
