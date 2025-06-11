package com.backend.softtrainer.events;

import lombok.Getter;

@Getter
public class HyperParameterUpdatedEvent {
    private final String userEmail;

    public HyperParameterUpdatedEvent(String userEmail) {
        this.userEmail = userEmail;
    }
} 