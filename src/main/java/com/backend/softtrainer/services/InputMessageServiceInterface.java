package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatDataDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.exceptions.SendMessageConditionException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 🎯 Input Message Service Interface
 * 
 * Common interface for both Legacy and Modern input message services.
 * This enables polymorphic usage through the factory pattern while
 * maintaining clean separation between the two processing approaches.
 */
public interface InputMessageServiceInterface {

    /**
     * 🎯 Process a user message and return the chat response
     */
    CompletableFuture<ChatDataDto> buildResponse(MessageRequestDto messageRequestDto) throws SendMessageConditionException;

    /**
     * 🎬 Initialize simulation messages from flow nodes
     */
    List<Message> getAndStoreMessageByFlow(List<FlowNode> flowNodes, Chat chat);
} 