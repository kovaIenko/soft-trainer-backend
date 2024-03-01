package com.backend.softtrainer.dtos;


import com.backend.softtrainer.entities.flow.FlowQuestion;

public record ChatResponseDto(String chatId, boolean success, String errorMessage, FlowQuestion rootFlowTask) {
}
