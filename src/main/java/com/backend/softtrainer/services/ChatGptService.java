package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatDto;
import com.backend.softtrainer.dtos.MessageDto;

import java.util.concurrent.CompletableFuture;

public interface ChatGptService {

  CompletableFuture<MessageDto> completeChat(final ChatDto chat);

}
