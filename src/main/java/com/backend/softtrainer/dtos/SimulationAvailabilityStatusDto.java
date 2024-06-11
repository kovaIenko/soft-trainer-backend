package com.backend.softtrainer.dtos;

public record SimulationAvailabilityStatusDto(Long id,
                                              String name,
                                              String avatar,
                                              boolean available,
                                              boolean completed,
                                              Long order) {
}
