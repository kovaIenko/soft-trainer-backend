package com.backend.softtrainer.services.notifications;

import com.backend.softtrainer.configs.TelegramProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Service
@RequiredArgsConstructor
public class TelegramService extends TelegramLongPollingBot {

    private final TelegramProperties telegramProperties;

    private TelegramBotsApi botsApi;

    @Override
    public void onUpdateReceived(org.telegram.telegrambots.meta.api.objects.Update update) {
        // Not needed for sending messages
    }

    @Override
    public String getBotUsername() {
        return telegramProperties.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return telegramProperties.getBotToken();
    }

    public void sendMessage(String text) {
        if (!telegramProperties.isEnabled()) {
            return;
        }

        try {
            if (botsApi == null) {
                botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(this);
            }

            SendMessage message = new SendMessage();
            message.setChatId(telegramProperties.getChatId());
            message.setText(text);
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
}
