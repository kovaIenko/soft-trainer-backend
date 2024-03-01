package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.services.ChatService;
import com.backend.softtrainer.services.FlowService;
import com.backend.softtrainer.services.MessageService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chats")
@AllArgsConstructor
public class ChatController {

  private final ChatService chatService;

  private final FlowService flowService;

  private final MessageService messageService;

  @PutMapping("/create")
  public ResponseEntity<ChatResponseDto> create(@RequestBody final ChatRequestDto chatRequestDto) {
    var flowTillActions = flowService.getFirstFlowQuestionsUntilActionable(chatRequestDto.getFlowName());

    if (!flowTillActions.isEmpty()) {
      var createdChat = chatService.store(chatRequestDto);

      var messages = messageService.getAndStoreMessageByFlow(flowTillActions, createdChat.getId());

      return ResponseEntity.ok(new ChatResponseDto(
        createdChat.getId(),
        true,
        "success",
        messages
      ));
    } else {
      return ResponseEntity.ok(new ChatResponseDto(
        chatRequestDto.getId(),
        false,
        String.format("No flow with name %s", chatRequestDto.getFlowName()),
        null
      ));
    }

  }

}
