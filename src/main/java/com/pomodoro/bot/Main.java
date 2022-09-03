package com.pomodoro.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

// Echo b Pomodoro bot с использованием библиотеки org.telegram:telegrambots:6.0.1
public class Main {

    private static final ConcurrentHashMap<Pomodoro.Timer, Long> userTimers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        var pomodoroBot = new Pomodoro();
        botsApi.registerBot(new Pomodoro());
        new Thread(() -> {
            try {
                pomodoroBot.run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).run();
    }

    static class Pomodoro extends TelegramLongPollingBot {

        enum TimerType {
            WORK,
            BREAK,
        }

        static record Timer(Instant time, TimerType timerType) {
        }
        private int count = 1;

        public Pomodoro() {
            super();
        }

        @Override
        public String getBotUsername() {
            return "Pomodoro bot";
        }

        @Override
        public String getBotToken() {
            return "5711497797:AAFyOOuPvUN5_j-2Mj9ZoZ9qmhOVDVdAIx4";
        }

        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String strMsg = update.getMessage().getText();
                String user = update.getMessage().getChat().getFirstName();
                Long chatId = update.getMessage().getChatId();
                String descriptionMsg = "Добро пожаловать, " + user + "\n Меня зовут, PomodoroBot, Pomodoro тайм-менеджмент!" +
                        "\nНапиши через пробел, время работы и время отдыха (в минутах) и количество повторений.";

                if (strMsg.equals("/start")) {
                    strMsg(chatId, descriptionMsg);
                    System.out.println(user + "использует PomodoroBot");
                    return;
                }
                var args = update.getMessage().getText().split(" ");

                strMsg(chatId, "Сейчас: " + LocalTime.now().getHour() + ":" + LocalTime.now().getMinute());
                strMsg(chatId, "Время работы: " + Long.parseLong(args[0]) + " мин");
                strMsg(chatId, "Время отдыха: " + Long.parseLong(args[1]) + " мин");

                if (args.length > 2) {
                    count = Integer.parseInt(args[2]);
                }

                strMsg(chatId, "Повторений: " + count + "\nПриступим!");
                timeSetting(chatId, args);
                System.out.println(userTimers.toString());
            }
        }

        private void timeSetting(Long chatId, String[] args) {
            Instant nowTime = Instant.now();
            for (int i = count; i > 0; i--) {
                Instant workTime = nowTime.plus(Long.parseLong(args[0]), ChronoUnit.MINUTES);
                Instant breakTime = workTime.plus(Long.parseLong(args[1]), ChronoUnit.MINUTES);
                nowTime = breakTime;
                userTimers.put(new Pomodoro.Timer(workTime, TimerType.WORK), chatId);
                userTimers.put(new Pomodoro.Timer(breakTime, TimerType.BREAK), chatId);
            }
        }

        private void strMsg(Long chatId, String text) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId);
            msg.setText(text);
            try {
                execute(msg);
            } catch (TelegramApiException e) {
                e.getStackTrace();
            }
        }

//        private void sendMsg(Long chatId, String msgStr) {
//            SendMessage msg = new SendMessage(chatId.toString(), msgStr);
//            try {
//                execute(msg);
//            } catch (TelegramApiException e) {
//                e.printStackTrace();
//            }
//        }

        public void run() throws InterruptedException {
            while (true) {
                System.out.printf("Количество таймеров пользователей " + userTimers.size() + "\n");
                userTimers.forEach((time, userId) -> {
                    System.out.printf("Проверка userId = %d, userTime = %s, now = %s\n", userId, time.toString(), Instant.now());
                    if (Instant.now().isAfter(time.time)) {

                        switch (time.timerType) {
                            case WORK -> strMsg(userId, "Пора отдыхать, осталось " + (userTimers.size()/2) + " повторений.");
                            case BREAK -> strMsg(userId, "Пора работать, осталось " + (userTimers.size()/2) + " повторений.");
                                }
                                userTimers.remove(time);
                            }
                });
                Thread.sleep(1000);
            }
        }
    }

//    static class EchoBot extends TelegramLongPollingBot {
//
//        @Override
//        public String getBotUsername() {
//            return "Echo bot";
//        }
//
//        @Override
//        public String getBotToken() {
//            return "5711497797:AAFyOOuPvUN5_j-2Mj9ZoZ9qmhOVDVdAIx4";
//        }
//
//        @Override
//        public void onUpdateReceived(Update update) {
//            if (update.hasMessage() && update.getMessage().hasText()) {
//                SendMessage msg = new SendMessage(update.getMessage().getChatId().toString(),
//                        update.getMessage().getText());
//                try {
//                    execute(msg);
//                } catch (TelegramApiException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
}

