package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.services.MessageService;
import com.backend.softtrainer.services.UserMessageService;
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

  private final UserMessageService userMessageService;

  @PutMapping("/send")
  public CompletableFuture<ResponseEntity<ChatResponseDto>> create(@RequestBody final MessageRequestDto messageRequestDto) {
    return messageService.buildResponse(messageRequestDto)
      .thenApply(messages -> ResponseEntity.ok(new ChatResponseDto(messageRequestDto.getChatId(), true, "success", userMessageService.combineMessages(messages))));
  }

}
