package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class AnonymousChatBot extends TelegramLongPollingBot {
    private static final Dotenv DOTENV = Dotenv.configure()
            .filename(".env")    // имя файла
            .ignoreIfMissing()    // если файла нет — не падаем
            .load();

    private static final String BOT_USERNAME = DOTENV.get("BOT_USERNAME").trim();
    private static final String BOT_TOKEN    = DOTENV.get("BOT_TOKEN").trim();

    private static volatile Long waitingUserId = null;
    private static final Map<Long, Long> chatPairs = new ConcurrentHashMap<>();

    AnonymousChatBot() {
        var commands = Arrays.asList(
                new BotCommand("/menu", "Показать меню анонимного чата"),
                new BotCommand("/start_chat", "Начать анонимный чат"),
                new BotCommand("/stop_chat", "Закончить чат")
        );
        try {
            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        }
        catch(TelegramApiException e){
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()){
            handleCallback(update.getCallbackQuery());
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()){
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText().trim();

            if("/menu".equals(text)){
                sendMenu(chatId);
                return;
            }

            if (chatPairs.containsKey(chatId)) {
                long parterId = chatPairs.get(chatId);
                forwardMessage(parterId, text);
            } else{
               sendText(chatId, "Вы не в чате. Откройте меню (/menu) и нажмите +" +
                       "'Начать чат', чтобы найти партнерa");
            }
        }
    }

    private void sendMenu(long chatId){
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText("Анонимный чат: выберите действие");

        InlineKeyboardButton startBtn = InlineKeyboardButton.builder()
                .text("Начать чат")
                .callbackData("START_CHAT")
                .build();
        InlineKeyboardButton stopBtn = InlineKeyboardButton.builder()
                .text("Закончить чат")
                .callbackData("STOP_CHAT")
                .build();

        InlineKeyboardMarkup markup =  InlineKeyboardMarkup.builder()
                .keyboardRow(Arrays.asList(startBtn, stopBtn))
                .build();
        msg.setReplyMarkup(markup);
        try {
            execute(msg);
        }
        catch(TelegramApiException e){
            e.printStackTrace();
        }
    }

    private void handleCallback(CallbackQuery callbackQuery){
        String data = callbackQuery.getData();
        String callbackId = callbackQuery.getId();
        long chatId = callbackQuery.getMessage().getChatId();

        try{
            execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .build());
        }
        catch(TelegramApiException e){
            e.printStackTrace();
        }

        switch(data) {
            case "START_CHAT":
                handleStart(chatId);
                break;
            case "STOP_CHAT":
                handleStop(chatId);
                break;
            default:
        }
    }

    private synchronized void handleStart(long chatId){
        if(chatPairs.containsKey(chatId)) {
            sendText(chatId, "Вы уже в чате");
            return;
        }
        if (waitingUserId == null){
            waitingUserId = chatId;
            sendText(chatId, "Ожидание партнера...");

        }else if (!waitingUserId.equals(chatId)) {
            long other = waitingUserId;
            chatPairs.put(chatId, other);
            chatPairs.put(other, chatId);
            waitingUserId = null;
            sendText(chatId, "Партнер найден! Пишите сообщение");
            sendText(other, "Партнер найден! Пишите сообщение");
        }
    }
    private synchronized void handleStop(long chatId){
        if(chatPairs.containsKey(chatId)) {
            long partner = chatPairs.remove(chatId);
            chatPairs.remove(partner);
            sendText(chatId, "Чат завершен!");
            sendText(partner, "Ваш партнер завершил чат");
        } else if (waitingUserId != null && waitingUserId.equals(chatId)) {
            waitingUserId = null;
            sendText(chatId, "Вы отменили поиск партнера");
        } else {
            sendText(chatId, "Вы не в чате");
        }
    }
    private void forwardMessage(long chatId, String text ){
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        try {
            execute(msg);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    private void sendText(long chatId, String text){
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        try {
            execute(msg);
        }
        catch(TelegramApiException e){
            e.printStackTrace();
        }
    }
}
