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

// Echo b Pomodoro bot ñ èñïîëüçîâàíèåì áèáëèîòåêè org.telegram:telegrambots:6.0.1
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
                String descriptionMsg = "Äîáðî ïîæàëîâàòü, " + user + "\n Ìåíÿ çîâóò, PomodoroBot, Pomodoro òàéì-ìåíåäæìåíò!" +
                        "\nÍàïèøè ÷åðåç ïðîáåë, âðåìÿ ðàáîòû è âðåìÿ îòäûõà (â ìèíóòàõ) è êîëè÷åñòâî ïîâòîðåíèé.";

                if (strMsg.equals("/start")) {
                    strMsg(chatId, descriptionMsg);
                    System.out.println(user + "èñïîëüçóåò PomodoroBot");
                    return;
                }
                var args = update.getMessage().getText().split(" ");

                strMsg(chatId, "Ñåé÷àñ: " + LocalTime.now().getHour() + ":" + LocalTime.now().getMinute());
                strMsg(chatId, "Âðåìÿ ðàáîòû: " + Long.parseLong(args[0]) + " ìèí");
                strMsg(chatId, "Âðåìÿ îòäûõà: " + Long.parseLong(args[1]) + " ìèí");

                if (args.length > 2) {
                    count = Integer.parseInt(args[2]);
                }

                strMsg(chatId, "Ïîâòîðåíèé: " + count + "\nÏðèñòóïèì!");
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

        public void run() throws InterruptedException {
            while (true) {
                System.out.printf("Êîëè÷åñòâî òàéìåðîâ ïîëüçîâàòåëåé " + userTimers.size() + "\n");
                userTimers.forEach((time, userId) -> {
                    System.out.printf("Ïðîâåðêà userId = %d, userTime = %s, now = %s\n", userId, time.toString(), Instant.now());
                    if (Instant.now().isAfter(time.time)) {

                        switch (time.timerType) {
                            case WORK -> strMsg(userId, "Ïîðà îòäûõàòü, îñòàëîñü " + (userTimers.size()/2) + " ïîâòîðåíèé.");
                            case BREAK -> strMsg(userId, "Ïîðà ðàáîòàòü, îñòàëîñü " + (userTimers.size()/2) + " ïîâòîðåíèé.");
                                }
                                userTimers.remove(time);
                            }
                });
                Thread.sleep(1000);
            }
        }
    }

}

