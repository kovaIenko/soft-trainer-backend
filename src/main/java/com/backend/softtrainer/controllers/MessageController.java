package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.MessageRequestDto;
import com.backend.softtrainer.dtos.MessageResponseDto;
import com.backend.softtrainer.services.MessageService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/message")
@AllArgsConstructor
public class MessageController {

  private final MessageService messageService;

  @PutMapping("/send")
  public CompletableFuture<ResponseEntity<MessageResponseDto>> create(@RequestBody final MessageRequestDto messageRequestDto) {
    return messageService.getResponse(messageRequestDto)
      .thenApply(message -> ResponseEntity.ok(new MessageResponseDto(message.getId(), message.getContent(), true)));
  }

}
