package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.exceptions.SendMessageConditionException;
import com.backend.softtrainer.services.MessageService;
import com.backend.softtrainer.services.UserMessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/message")
@AllArgsConstructor
@Slf4j
public class MessageController {

  private final MessageService messageService;

  private final UserMessageService userMessageService;

  @PutMapping("/send")
  @PreAuthorize("@customUsrDetailsService.isResourceOwner(authentication, #messageRequestDto?.ownerId)")
  public CompletableFuture<ResponseEntity<ChatResponseDto>> create(@RequestBody MessageRequestDto messageRequestDto) {

    try {
      return messageService.buildResponse(messageRequestDto)
        .thenApply(messages -> ResponseEntity.ok(new ChatResponseDto(
          messageRequestDto.getChatId(),
          null,
          true,
          "success",
          userMessageService.combineMessages(messages)
        )));

    } catch (SendMessageConditionException e) {

      log.error(e.getMessage());
      return CompletableFuture.completedFuture(
        ResponseEntity.ok(new ChatResponseDto(
          messageRequestDto.getChatId(),
          null,
          false,
          e.getMessage(),
          Collections.emptyList()
        )));
    }
  }

}
