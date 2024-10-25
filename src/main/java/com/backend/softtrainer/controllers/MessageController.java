package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.dtos.client.UserEnterTextMessageDto;
import com.backend.softtrainer.dtos.client.UserMessageDto;
import com.backend.softtrainer.dtos.client.UserMultiChoiceTaskMessageDto;
import com.backend.softtrainer.dtos.client.UserSingleChoiceMessageDto;
import com.backend.softtrainer.dtos.client.UserSingleChoiceTaskMessageDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.exceptions.SendMessageConditionException;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.services.InputMessageService;
import com.backend.softtrainer.services.UserMessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/message")
@AllArgsConstructor
@Slf4j
public class MessageController {

  private final InputMessageService inputMessageService;

  private final UserMessageService userMessageService;

  private final ChatRepository chatRepository;

  @PutMapping("/send")
  @PreAuthorize("@customUsrDetailsService.isChatOfUser(authentication, #messageRequestDto?.chatId)")
  public CompletableFuture<ResponseEntity<ChatResponseDto>> create(@RequestBody MessageRequestDto messageRequestDto) {
    try {
      return inputMessageService.buildResponse(messageRequestDto)
        .thenApply(chatData -> {

          var prevHearts = chatData.params().getHearts();
          var combinedMessage = userMessageService.combineMessages(chatData.messages(), chatData.params());

          if (Objects.nonNull(prevHearts) && !Objects.equals(prevHearts, chatData.params().getHearts())) {
            chatRepository.updateHearts(messageRequestDto.getChatId(), chatData.params().getHearts());
          }

          var chatOptional = chatRepository.findById(messageRequestDto.getChatId());

          chatOptional.ifPresent(chat -> {
            if (Objects.nonNull(chatData.params().getHearts()) && chatData.params().getHearts() <= 0.0) {
              log.info("Remove all non-interacted messages for the chat {}", chat.getId());
              removeNonInteractedMessages(combinedMessage);
              log.info("User {} has used already all the hearts for chat {}", chat.getUser().getId(), chat.getId());
              var resultMsg = inputMessageService.generateLastSimulationMessage(chat);
              var userResultMsg = userMessageService.convert(resultMsg, null);
              log.info("The last message for the chat with the specific message looks like : {}", userResultMsg);
              combinedMessage.addAll(userResultMsg.toList());
            }
          });

          return ResponseEntity.ok(new ChatResponseDto(
            messageRequestDto.getChatId(),
            null,
            true,
            "success",
            combinedMessage,
            chatData.params()
          ));
        });
    } catch (SendMessageConditionException e) {
      log.error(e.getMessage());
      return CompletableFuture.completedFuture(
        ResponseEntity.ok(new ChatResponseDto(
          messageRequestDto.getChatId(),
          null,
          false,
          e.getMessage(),
          Collections.emptyList(),
          null
        )));
    }
  }

  @PostMapping("/get")
  @PreAuthorize("@customUsrDetailsService.isChatOfUser(authentication, #messageRequestDto?.chatId)")
  public CompletableFuture<ResponseEntity<ChatResponseDto>> getHintMessage(@RequestBody MessageRequestDto messageRequestDto) {
    try {
      return inputMessageService.buildResponse(messageRequestDto)
        .thenApply(chatData -> {

          log.info(
            "The messages for the chat with the specific message looks like : {} when size is {}",
            chatData.messages(),
            chatData.messages().size()
          );

          var prevHearts = chatData.params().getHearts();

          var combinedMessage = userMessageService.combineMessage(chatData.messages(), chatData.params());

          if (!Objects.equals(prevHearts, chatData.params().getHearts())) {
            chatRepository.updateHearts(messageRequestDto.getChatId(), chatData.params().getHearts());
          }
          var chatResponse = new ChatResponseDto(
            messageRequestDto.getChatId(),
            null,
            true,
            "success",
            combinedMessage,
            chatData.params()
          );

          log.info(
            "The chat with the specific message looks like : {} when combined messages: {}",
            chatResponse,
            combinedMessage
          );
          return ResponseEntity.ok(chatResponse);
        });
    } catch (SendMessageConditionException e) {
      log.error(e.getMessage());
      return CompletableFuture.completedFuture(
        ResponseEntity.ok(new ChatResponseDto(
          messageRequestDto.getChatId(),
          null,
          false,
          e.getMessage(),
          Collections.emptyList(),
          null
        )));
    }
  }

  private void removeNonInteractedMessages(List<UserMessageDto> messages) {
    var listIterator = messages.listIterator(messages.size());
    while (listIterator.hasPrevious()) {
      UserMessageDto msg = listIterator.previous();
      if (!isActionableMsgInteracted(msg)) {
        listIterator.remove();
      } else {
        break;
      }
    }
  }

  private boolean isActionableMsgInteracted(UserMessageDto msg) {
    if (msg instanceof UserSingleChoiceMessageDto userSingleChoiceMessageDto) {
      return userSingleChoiceMessageDto.isVoted();
    } else if (msg instanceof UserSingleChoiceTaskMessageDto userSingleChoiceTaskMessageDto) {
      return userSingleChoiceTaskMessageDto.isVoted();
    } else if (msg instanceof UserMultiChoiceTaskMessageDto userMultiChoiceTaskMessageDto) {
      return userMultiChoiceTaskMessageDto.isVoted();
    } else if (msg instanceof UserEnterTextMessageDto userEnterTextMessageDto) {
      return userEnterTextMessageDto.isVoted();
    }
    return false;
  }

}
