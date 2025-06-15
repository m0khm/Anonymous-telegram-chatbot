// Main.java
package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new AnonymousChatBot());
            System.out.println("Анонимный чат-бот запущен!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
