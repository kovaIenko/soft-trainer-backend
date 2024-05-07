package com.backend.softtrainer.dtos;

public record SimulationResponseDto(Long id,
                                    String name,
                                    String avatar,
                                    boolean available,
                                    boolean completed,
                                    Long order) {
}
