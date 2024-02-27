package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.MessageRequestDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Message;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.utils.Converter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
public class MessageService {

  private ChatGptService chatGptService;

  private final ChatRepository chatRepository;
  private final MessageRepository messageRepository;

  /**
   * method accepts the message from the user and return answer from the chatgpt
   *
   * @param messageRequestDto
   * @return
   */
  public CompletableFuture<Message> getResponse(final MessageRequestDto messageRequestDto) {

    //store user's message
    var messageEntity = Converter.convert(messageRequestDto);
    messageRepository.save(messageEntity);


    var optionalChat = chatRepository.findByIdWithMessages(messageRequestDto.getChatId());

    Chat chat = optionalChat.orElseThrow(() -> new NoSuchElementException(String.format(
      "No chat with id %s",
      messageRequestDto.getChatId()
    )));

    //get response from chatgpt, store it and return to front
    return chatGptService.completeChat(Converter.convert(chat))
      .thenApply(messageDto -> Message.builder()
         .id(UUID.randomUUID().toString())
         .content(messageDto.content())
         .timestamp(LocalDateTime.now())
         .chatId(messageRequestDto.getChatId())
         .build())
      .thenApply(messageRepository::save);
  }

}
