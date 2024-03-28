package com.backend.softtrainer.dtos.flow;

import java.util.Set;

public record AllFlowsResponseDto(Set<String> names, boolean success) {
}
