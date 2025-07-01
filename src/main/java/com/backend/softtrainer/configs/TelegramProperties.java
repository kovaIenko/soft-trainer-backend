package com.backend.softtrainer.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private String botToken;
    private String botUsername;
    private String chatId;
    private boolean enabled = true;
}
