package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.dtos.client.UserEnterTextMessageDto;
import com.backend.softtrainer.dtos.client.UserMessageDto;
import com.backend.softtrainer.dtos.client.UserMultiChoiceTaskMessageDto;
import com.backend.softtrainer.dtos.client.UserSingleChoiceMessageDto;
import com.backend.softtrainer.dtos.client.UserSingleChoiceTaskMessageDto;
import com.backend.softtrainer.dtos.client.UserTextMessageDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.exceptions.SendMessageConditionException;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.services.UserMessageService;
import com.backend.softtrainer.simulation.DualModeSimulationRuntime;
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
import java.util.UUID;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/message")
@AllArgsConstructor
@Slf4j
public class MessageController {

  private final UserMessageService userMessageService;

  private final ChatRepository chatRepository;

  private final DualModeSimulationRuntime dualModeSimulationRuntime;

  @PutMapping("/send")
  @PreAuthorize("@customUsrDetailsService.isChatOfUser(authentication, #messageRequestDto?.chatId)")
  public CompletableFuture<ResponseEntity<ChatResponseDto>> create(@RequestBody MessageRequestDto messageRequestDto) {
    return dualModeSimulationRuntime.processUserMessage(messageRequestDto)
        .thenApply(chatData -> {
          var prevHearts = chatData.params().getHearts();
          var combinedMessage = userMessageService.combineMessages(chatData.messages(), chatData.params());

          // Update hearts in a new transaction if needed
          if (Objects.nonNull(prevHearts) && !Objects.equals(prevHearts, chatData.params().getHearts())) {
            chatRepository.updateHearts(messageRequestDto.getChatId(), chatData.params().getHearts());
          }

          // Handle zero hearts case in a new transaction
          var chatOptional = chatRepository.findById(messageRequestDto.getChatId());
          if (chatOptional.isPresent() && Objects.nonNull(chatData.params().getHearts()) 
              && chatData.params().getHearts() <= 0.0) {
            
            var chat = chatOptional.get();
            log.info("Remove all non-interacted messages for the chat {}", chat.getId());
            removeNonInteractedMessages(combinedMessage);
            log.info("User {} has used already all the hearts for chat {}", 
                    chat.getUser().getId(), chat.getId());
            
            // Generate last message in a separate transaction
            try {
                var resultMsg = dualModeSimulationRuntime.generateLastSimulationMessage(chat).get();
                if (resultMsg != null) {
                    var userResultMsg = userMessageService.convert(resultMsg, null);
                    log.info("Successfully generated last simulation message: {}", userResultMsg);
                    combinedMessage.addAll(userResultMsg.toList());
                }
            } catch (Exception e) {
                log.error("Failed to generate last simulation message", e);
                // Add a generic message instead of failing
                var fallbackMsg = UserTextMessageDto.builder()
                    .id(UUID.randomUUID().toString())
                    .messageType(MessageType.TEXT)
                    .content("No more attempts remaining. Please try again.")
                    .timestamp(LocalDateTime.now())
                    .build();
                combinedMessage.add(fallbackMsg);
            }
          }

          return ResponseEntity.ok(new ChatResponseDto(
            messageRequestDto.getChatId(),
            null,
            true,
            "success (dual-mode runtime)",
            combinedMessage,
            chatData.params()
          ));
        })
        .exceptionally(throwable -> {
          log.error("❌ Dual-mode runtime failed completely", throwable);
          return ResponseEntity.ok(new ChatResponseDto(
            messageRequestDto.getChatId(),
            null,
            false,
            "Processing failed: " + throwable.getMessage(),
            Collections.emptyList(),
            null
          ));
        });
  }

  @PostMapping("/get")
  @PreAuthorize("@customUsrDetailsService.isChatOfUser(authentication, #messageRequestDto?.chatId)")
  public CompletableFuture<ResponseEntity<ChatResponseDto>> getHintMessage(@RequestBody MessageRequestDto messageRequestDto) {
    return dualModeSimulationRuntime.processUserMessage(messageRequestDto)
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
            "success (dual-mode runtime)",
            combinedMessage,
            chatData.params()
          );

          log.info(
            "The chat with the specific message looks like : {} when combined messages: {}",
            chatResponse,
            combinedMessage
          );
          return ResponseEntity.ok(chatResponse);
        })
        .exceptionally(throwable -> {
          log.error("❌ Dual-mode runtime failed for hint", throwable);
          return ResponseEntity.ok(new ChatResponseDto(
            messageRequestDto.getChatId(),
            null,
            false,
            "Hint processing failed: " + throwable.getMessage(),
            Collections.emptyList(),
            null
          ));
        });
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
