package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.MessageRequestDto;
import com.backend.softtrainer.dtos.MessageResponseDto;
import com.backend.softtrainer.services.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/message")
public class MessageController {

  private MessageService messageService;

  @PutMapping("/create")
  public ResponseEntity<MessageResponseDto> create(@RequestBody final MessageRequestDto messageRequestDto) {
    var created  = messageService.store(messageRequestDto);
    return ResponseEntity.ok(new MessageResponseDto(created.getId(), true));
  }

}
