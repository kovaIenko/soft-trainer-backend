package com.backend.softtrainer.dtos;

import java.util.Set;

public record AllFlowsResponseDto(Set<String> names, boolean success) {
}
