package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.dtos.ChatsResponseDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.services.ChatService;
import com.backend.softtrainer.services.FlowService;
import com.backend.softtrainer.services.MessageService;
import com.backend.softtrainer.services.UserMessageService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chats")
@AllArgsConstructor
public class ChatController {

  private final ChatService chatService;

  private final FlowService flowService;

  private final MessageService messageService;

  private final UserMessageService userMessageService;

  @PutMapping("/create")
  public ResponseEntity<ChatResponseDto> create(@RequestBody final ChatRequestDto chatRequestDto) {
    if (chatService.existsBy(chatRequestDto.getOwnerId(), chatRequestDto.getFlowName())) {
      return ResponseEntity.ok(new ChatResponseDto(
        null,
        false,
        String.format(
          "Chat already exists for user %s and for training %s",
          chatRequestDto.getOwnerId(),
          chatRequestDto.getFlowName()
        ),
        null
      ));
    }
    var flowTillActions = flowService.getFirstFlowNodesUntilActionable(chatRequestDto.getFlowName());

    if (!flowTillActions.isEmpty()) {
      var createdChat = chatService.store(chatRequestDto);

      var messages = messageService.getAndStoreMessageByFlow(flowTillActions, createdChat.getId()).stream().toList();
      var combinedMessages = userMessageService.combineMessages(messages);
      return ResponseEntity.ok(new ChatResponseDto(
        createdChat.getId(),
        true,
        "success",
        combinedMessages
      ));

    } else {
      return ResponseEntity.ok(new ChatResponseDto(
        null,
        false,
        String.format("No flow with name %s", chatRequestDto.getFlowName()),
        null
      ));
    }
  }

  @GetMapping("/get")
  public ResponseEntity<ChatResponseDto> get(@RequestParam(name = "ownerId") Long ownerId,
                                             @RequestParam(name = "flowName") String flowName) {
    var chatOptional = chatService.findChatWithMessages(ownerId, flowName);

    if (chatOptional.isEmpty()) {
      return ResponseEntity.ok(new ChatResponseDto(
        null,
        false,
        String.format(
          "Chat doesn't exists for user %s and for training %s",
          ownerId,
          flowName
        ),
        null
      ));
    }

    var chat = chatOptional.get();

    var messages = chat.getMessages().stream().toList();

    var combinedMessages = userMessageService.combineMessages(messages);

    return ResponseEntity.ok(new ChatResponseDto(
      chat.getId(),
      true,
      "success",
      combinedMessages
    ));
  }

  @GetMapping("/get/all")
  public ResponseEntity<ChatsResponseDto> getAll(@RequestParam(name = "ownerId") Long ownerId) {
    var chats = chatService.getAll(ownerId);

    return ResponseEntity.ok(new ChatsResponseDto(
      chats.stream().map(Chat::getFlowName).toList(),
      true,
      "success"
    ));
  }

}
