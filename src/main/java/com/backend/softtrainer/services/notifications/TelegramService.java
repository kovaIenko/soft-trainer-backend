package com.backend.softtrainer.services.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final NotificationBot bot;
    
    @Value("${telegram.notifications.enabled:true}")
    private boolean notificationsEnabled;

    public void sendMessage(String text) {
        if (notificationsEnabled) {
            bot.sendMessage(text);
        }
    }
} 