package com.backend.softtrainer.dtos;

import java.util.List;

public record ChatsResponseDto(List<String> names, boolean success, String errorMessage) {}

