package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.services.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/chats")
public class ChatController {

  private ChatService chatService;

  @PutMapping("/create")
  public ResponseEntity<ChatResponseDto> create(@RequestBody final ChatRequestDto chatRequestDto) {
    var createdChat  = chatService.store(chatRequestDto);
    return ResponseEntity.ok(new ChatResponseDto(createdChat.getId(), true));
  }

  @PutMapping("/update")
  public ResponseEntity<ChatResponseDto> update(@RequestBody final ChatRequestDto chatRequestDto) {
    return ResponseEntity.ok(new ChatResponseDto(chatRequestDto.getId(), true));
  }

}
