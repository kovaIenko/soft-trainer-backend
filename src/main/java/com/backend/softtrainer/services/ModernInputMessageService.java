package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.ChatDataDto;
import com.backend.softtrainer.dtos.ChatParams;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.exceptions.SendMessageConditionException;
import com.backend.softtrainer.repositories.ChatRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 🚀 Modern Input Message Service
 * 
 * Specialized service for processing modern simulations that use flow_rules 
 * instead of legacy show_predicate logic. This is a simplified implementation
 * that delegates to the original InputMessageService for now, but can be
 * enhanced with modern-specific logic in the future.
 */
@Service
@AllArgsConstructor
@Slf4j
public class ModernInputMessageService implements InputMessageServiceInterface {

    private final ChatRepository chatRepository;
    private final InputMessageService originalInputMessageService;

    @Override
    public CompletableFuture<ChatDataDto> buildResponse(final MessageRequestDto messageRequestDto) throws SendMessageConditionException {
        log.info("🚀 ModernInputMessageService processing message {} for chat {}", 
                messageRequestDto.getId(), messageRequestDto.getChatId());
        
        // For now, delegate to the original service
        // In the future, this can be enhanced with modern-specific flow_rules logic
        return originalInputMessageService.buildResponse(messageRequestDto);
    }

    @Override
    public List<Message> getAndStoreMessageByFlow(final List<FlowNode> flowNodes, final Chat chat) {
        log.info("🔄 ModernInputMessageService converting {} flow nodes to messages", flowNodes.size());
        
        // For now, delegate to the original service
        // In the future, this can be enhanced with modern-specific flow_rules logic
        return originalInputMessageService.getAndStoreMessageByFlow(flowNodes, chat);
    }
} 