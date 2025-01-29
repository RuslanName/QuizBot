package mainFiles.services;

import mainFiles.configs.BotConfig;

import mainFiles.database.tables.user.User;
import mainFiles.database.tables.user.UsersRepository;
import mainFiles.database.tables.differentState.DifferentState;
import mainFiles.database.tables.differentState.DifferentStatesRepository;
import mainFiles.database.tables.quizSetting.QuizSetting;
import mainFiles.database.tables.quizSetting.QuizSettingsRepository;
import mainFiles.database.tables.question.Question;
import mainFiles.database.tables.question.QuestionsRepository;
import mainFiles.database.tables.answerOption.AnswerOption;
import mainFiles.database.tables.answerOption.AnswerOptionsRepository;
import mainFiles.database.tables.quizState.QuizStatesRepository;
import mainFiles.database.tables.quizState.QuizState;
import mainFiles.database.tables.userResuit.UserResult;
import mainFiles.database.tables.userResuit.UserResultsRepository;
import mainFiles.database.tables.userPrize.QuizPrize;
import mainFiles.database.tables.userPrize.QuizPrizesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private DifferentStatesRepository differentStatesRepository;

    @Autowired
    private QuestionsRepository questionsRepository;

    @Autowired
    private AnswerOptionsRepository answerOptionsRepository;

    @Autowired
    private UserResultsRepository userResultsRepository;

    @Autowired
    private QuizStatesRepository quizStatesRepository;

    @Autowired
    private QuizSettingsRepository quizSettingsRepository;

    @Autowired
    private QuizPrizesRepository quizPrizesRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    final BotConfig config;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    static final String QUIZ_CREATE_KEYBOARD = "Создать квиз";
    static final String QUIZ_UPDATE_KEYBOARD = "Изменить квиз";
    static final String SHOW_USERS_LEADERBOARD_KEYBOARD = "Список лидеров";
    static final String SHOW_STATISTIC_KEYBOARD = "Статистика";
    static final String USER_START_QUIZ_KEYBOARD = "Начать прохождение квиза";


    static final String BETBOOM_ACCOUNT_REGISTRATION_BUTTON = "Зарегистрироваться в BetBoom";
    static final String NO_BUTTON = "Нет";
    static final String YES_BUTTON = "Да";
    static final String CANCEL_BUTTON = "Отмена";
    static final String QUIZ_UPDATE_TIME_LIMIT_BUTTON = "Время";
    static final String QUIZ_UPDATE_PRIZE_BUTTON = "Приз";

    static final String NO_CHANNEL_FOLLOW_TEXT = bold("Вы не подписаны на канал: https://t.me/roganov_hockey") +
            " \n" + italic("Подпишитесь, и сможете пользоваться ботом");

    public TelegramBot(BotConfig config) {
        this.config = config;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "запуск бота"));
        listOfCommands.add(new BotCommand("/registration", "регистрация"));
        listOfCommands.add(new BotCommand("/help", "информация о возможностях бота"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }

        scheduleTask();
    }

    private void scheduleTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                executorService.submit(() -> {
                    try {
                        checkQuizTimeLimitEnd();
                    } catch (Exception e) {
                        log.error("Error in scheduled task: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                log.error("Error in scheduling task: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("Received update: {}", update);

        if (update.hasMessage()) {
            Message message = update.getMessage();

            long chatId = message.getChatId();

            if (isUserInChannel(chatId)) {
                if (message.hasText()) {
                    String text = message.getText();

                    if (text.equals("/start") || text.equals("/registration")) {
                        if (usersRepository.existsById(chatId)) {
                            if (text.equals("/start")) {
                                start(chatId);
                            }

                            else {
                                sendMessage(chatId, "Вы уже зарегистрированы");
                            }
                        }

                        else {
                            setState(chatId, ActionType.REGISTRATION);
                            sendRegistrationMessage(chatId);
                        }
                    }

                    else if (text.equals("/help")) {
                        text = boldAndUnderline("СПИСОК КОМАНД") + "\n\n" +
                                "/start - запустить бота \n" +
                                "/registration - зарегестрироваться \n" +
                                "/help - показать информацию о возможностях бота \n";

                        sendMessage(chatId, text);
                    }

                    else if (text.equals("/photo")) {
                        long startTime = System.currentTimeMillis();

                        sendPhoto(chatId, config.getRegistrationImagePath(), "");
                        sendPhoto(chatId, config.getRegistrationImagePath(), "");
                        sendPhoto(chatId, config.getRegistrationImagePath(), "");
                        sendPhoto(chatId, config.getRegistrationImagePath(), "");
                        sendPhoto(chatId, config.getRegistrationImagePath(), "");

                        long endTime = System.currentTimeMillis();
                        long timeTaken = endTime - startTime;

                        sendMessage(chatId, timeTaken);
                    }


                    else if (text.equals(QUIZ_CREATE_KEYBOARD) && isOwner(chatId)) {
                        if (questionsRepository.existsById(1)) {
                            setState(chatId, ActionType.QUIZ_START_CREATE_QUESTION);
                            sendQwizStartCreateQuestionMessage(chatId);
                        }

                        else {
                            setState(chatId, ActionType.QUIZ_CREATE);
                            sendMessage(chatId, "Введите " + getNextId("questions_data") + " вопрос");
                        }
                    }

                    else if (text.equals(QUIZ_UPDATE_KEYBOARD) && isOwner(chatId)) {
                        setState(chatId, ActionType.QUIZ_UPDATE_QUESTION);
                        sendQwizUpdateQuestionMessage(chatId);
                    }

                    else if (text.equals(SHOW_USERS_LEADERBOARD_KEYBOARD) && isOwner(chatId)) {
                        List<UserResult> leaderboard = userResultsRepository.findLeaderboardUserResults();

                        if (leaderboard.isEmpty()) {
                            sendMessage(chatId, "Список лидеров пуст");
                        }

                        else {
                            StringBuilder leaderboardMessage = new StringBuilder(boldAndUnderline("СПИСОК ЛИДЕРОВ\n\n"));

                            int i = 1;
                            for (UserResult userResult : leaderboard) {
                                User user = usersRepository.findById(userResult.getChatId()).get();
                                leaderboardMessage.append(String.format("%d) @%s - Ответы: %d, Время: %s | Betboom ID: %d\n",
                                        i++, user.getUserName(), userResult.getResult(), userResult.getTimeSpent(), user.getBetboomId()));
                            }

                            sendMessage(chatId, leaderboardMessage.toString());
                        }
                    }

                    else if (text.equals(SHOW_STATISTIC_KEYBOARD) && isOwner(chatId)) {
                        List<User> users = (List<User>) usersRepository.findAll();

                        if (users.isEmpty()) {
                            sendMessage(chatId, "Статистика отсутствует");
                        }

                        else {
                            LocalDateTime now = LocalDateTime.now();

                            long dayUsersCount = users.stream()
                                    .filter(user -> isSameDay(user.getRegisteredAt(), now))
                                    .count();

                            long weekUsersCount = users.stream()
                                    .filter(user -> isSameWeek(user.getRegisteredAt(), now))
                                    .count();

                            StringBuilder statisticMessage = new StringBuilder(boldAndUnderline("СТАТИСТИКА\n\n"));

                            statisticMessage.append(italic("Количество зарегистрировавшихся людей")).append("\n");
                            statisticMessage.append("За сегодня: ").append(dayUsersCount).append("\n");
                            statisticMessage.append("За неделю: ").append(weekUsersCount).append("\n");
                            statisticMessage.append("За все время: ").append(usersRepository.count()).append("\n");

                            List<UserResult> userResults = (List<UserResult>) userResultsRepository.findAll();

                            if (!userResults.isEmpty()) {
                                long dayUserResultsCount = userResults.stream()
                                        .filter(userResult -> isSameDay(userResult.getRegisteredAt(), now))
                                        .count();

                                long weekUserResultsCount = userResults.stream()
                                        .filter(userResult -> isSameWeek(userResult.getRegisteredAt(), now))
                                        .count();

                                statisticMessage.append("\n");
                                statisticMessage.append(italic("Количество людей, прошедших квиз")).append("\n");
                                statisticMessage.append("За сегодня: ").append(dayUserResultsCount).append("\n");
                                statisticMessage.append("За неделю: ").append(weekUserResultsCount).append("\n");
                                statisticMessage.append("За все время: ").append(userResultsRepository.count()).append("\n");
                            }

                            sendMessage(chatId, statisticMessage.toString());
                        }
                    }

                    else if (text.equals(USER_START_QUIZ_KEYBOARD) && usersRepository.existsById(chatId)) {
                        if (questionsRepository.existsById(1)) {
                            if (userResultsRepository.existsById(chatId)) {
                                sendMessage(chatId, "Вы уже прошли квиз");
                            }

                            else {
                                Optional<QuizSetting> quizSetting = quizSettingsRepository.findById(1);

                                if (quizSetting.isPresent() && (!isCurrentTimeLower(quizSetting.get().getTimeLimit()) || quizSetting.get().isWinnersNotification())) {
                                    sendMessage(chatId, "Квиз больше нельзя пройти");
                                }

                                else {
                                    Optional<DifferentState> optionalState = differentStatesRepository.findById(chatId);

                                    if (optionalState.isPresent() && optionalState.get().getState() == ActionType.USER_PASS_QUIZ.getCode()) {
                                        sendMessage(chatId, "Вы уже проходите квиз");
                                    }

                                    else {
                                        setState(chatId, ActionType.USER_START_QUIZ_QUESTION);
                                        sendUserStartQuizQuestionMessage(chatId);
                                    }
                                }
                            }
                        }

                        else {
                            sendMessage(chatId, "На данный момент квиз отсутствует");
                        }
                    }

                    else if (differentStatesRepository.existsById(chatId)) {
                        if (differentStatesRepository.findById(chatId).get().getState() == ActionType.REGISTRATION.getCode()) {
                            if (isNumeric(text)) {
                                if (isUniqueBetboomId(text)) {
                                    registration(message);

                                    deleteState(chatId);
                                }

                                else {
                                    sendMessage(chatId, "Уже есть пользователь с данным Betboom ID. Проверьте корректность ввода и введите снова");
                                }
                            }

                            else {
                                sendMessage(chatId, "Betboom ID введён неправильно. Проверьте корректность ввода и введите снова");
                            }
                        }

                        else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.QUIZ_CREATE.getCode()) {
                            if (isValidQuizQuestionFormat(message)) {
                                try {
                                    addQuestionDatabase(message);
                                } catch (TelegramApiException e) {
                                    throw new RuntimeException(e);
                                } catch (MalformedURLException e) {
                                    throw new RuntimeException(e);
                                }

                                setState(chatId, ActionType.QUIZ_CONTINUE_CREATE_QUESTION);
                                sendQwizContinueCreateQuestionMessage(chatId);
                            }

                            else {
                                sendMessage(chatId, "Вопрос введён неправильно. Проверьте корректность ввода и введите снова");
                            }
                        }

                        else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.QUIZ_ADD_TIME_LIMIT.getCode()) {
                            if (isValidQuizDateFormat(text)) {
                                addQuizTimeLimitDatabase(chatId, text);

                                setState(chatId, ActionType.QUIZ_ADD_PRIZE);
                                sendMessage(chatId, "Введите призы за квиз");
                            }

                            else {
                                sendMessage(chatId, "Время введено неправильно. Проверьте корректность ввода и введите снова");
                            }
                        }

                        else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.QUIZ_ADD_PRIZE.getCode()) {
                            if (isValidQuizPrizeFormat(text)) {
                                addQuizPrizeDatabase(chatId, text);

                                sendMessage(chatId, "Создание квиза завершено");

                                for (User user : usersRepository.findAll()) {
                                    long userChatID = user.getChatId();

                                    if (!isOwner(userChatID)) {
                                        sendMessage(userChatID, "Добавлен новый квиз. Сообщение с результатами " +
                                                convertQuizLimitTimeToString(quizSettingsRepository.findById(1).get().getTimeLimit()));
                                    }
                                }

                                deleteState(chatId);
                            }

                            else {
                                sendMessage(chatId, "Призы введены неправильно. Проверьте корректность ввода и введите снова");
                            }
                        }

                        else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.QUIZ_UPDATE_TIME_LIMIT.getCode()) {
                            if (isValidQuizDateFormat(text)) {
                                updateQuizTimeLimitDatabase(chatId, text);

                                sendMessage(chatId, "Время обновлено");

                                deleteState(chatId);
                            }

                            else {
                                sendMessage(chatId, "Время введено неправильно. Проверьте корректность ввода и введите снова");
                            }
                        }

                        else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.QUIZ_UPDATE_PRIZE.getCode()) {
                            if (isValidQuizPrizeFormat(text)) {
                                updateQuizPrizeDatabase(chatId, text);

                                sendMessage(chatId, "Призы обновлены");

                                deleteState(chatId);
                            }

                            else {
                                sendMessage(chatId, "Призы введены неправильно. Проверьте корректность ввода и введите снова");
                            }
                        }
                    }
                }

                else if (message.hasPhoto()) {
                    if (differentStatesRepository.existsById(chatId)) {
                        if (differentStatesRepository.findById(chatId).get().getState() == ActionType.QUIZ_CREATE.getCode() && isValidQuizQuestionFormat(message)) {
                            try {
                                addQuestionDatabase(message);
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }

                            setState(chatId, ActionType.QUIZ_CONTINUE_CREATE_QUESTION);
                            sendQwizContinueCreateQuestionMessage(chatId);
                        }

                        else {
                            sendMessage(chatId, "Вопрос введён неправильно. Проверьте корректность ввода и введите снова");
                        }
                    }
                }
            }

            else {
                sendMessage(chatId, NO_CHANNEL_FOLLOW_TEXT);
            }
        }

        else if (update.hasCallbackQuery()) {
            Message message = update.getCallbackQuery().getMessage();

            long chatId = message.getChatId();

            if (isUserInChannel(chatId)) {
                int messageId = message.getMessageId();

                String callbackData = update.getCallbackQuery().getData();

                if (differentStatesRepository.findById(chatId).get().getState() == ActionType.QUIZ_START_CREATE_QUESTION.getCode()) {
                    if (callbackData.equals(CANCEL_BUTTON)) {
                        editMessageText(chatId, messageId, "Создание нового квиза отменено");

                        deleteState(chatId);
                    }

                    else if (callbackData.equals(YES_BUTTON)) {
                        try {
                            deleteQuiz();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        setState(chatId, ActionType.QUIZ_CREATE);
                        editMessageText(chatId, messageId, "Предыдущий квиз удалён");
                        sendMessage(chatId, "Введите " + getNextId("questions_data") + " вопрос");
                    }
                }

                else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.QUIZ_CONTINUE_CREATE_QUESTION.getCode()) {
                    if (callbackData.equals(NO_BUTTON)) {
                        setState(chatId, ActionType.QUIZ_ADD_TIME_LIMIT);
                        editMessageText(chatId, messageId, "Введите ограничение времени для прохождения квиза");
                    }

                    else if (callbackData.equals(YES_BUTTON)) {
                        setState(chatId, ActionType.QUIZ_CREATE);
                        editMessageText(chatId, messageId, "Введите " + getNextId("questions_data") + " вопрос");
                    }
                }

                else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.QUIZ_UPDATE_QUESTION.getCode()) {
                    if (callbackData.equals(CANCEL_BUTTON)) {
                        editMessageText(chatId, messageId, "Изменение квиза отменено");

                        deleteState(chatId);
                    }

                    else if (callbackData.equals(QUIZ_UPDATE_TIME_LIMIT_BUTTON)) {
                        setState(chatId, ActionType.QUIZ_UPDATE_TIME_LIMIT);
                        editMessageText(chatId, messageId, "Введите ограничение времени для прохождения квиза");
                    }

                    else if (callbackData.equals(QUIZ_UPDATE_PRIZE_BUTTON)) {
                        setState(chatId, ActionType.QUIZ_UPDATE_PRIZE);
                        editMessageText(chatId, messageId, "Введите призы за квиз");
                    }
                }

                else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.USER_START_QUIZ_QUESTION.getCode()) {
                    if (callbackData.equals(CANCEL_BUTTON)) {
                        editMessageText(chatId, messageId, "Прохождение квиза отменено");

                        deleteState(chatId);
                    }

                    else if (callbackData.equals(YES_BUTTON)) {
                        setState(chatId, ActionType.USER_PASS_QUIZ);
                        editMessageText(chatId, messageId, "Прожождение квиза начато");
                        sendQuizPassingQuestionMessage(chatId, messageId);
                    }
                }

                else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.USER_PASS_QUIZ.getCode()) {
                    if (callbackData.matches("ANSWER_OPTION_\\d+_BUTTON")) {
                        int id = extractNumberFromAnswerOptionButton(callbackData);

                        QuizState quizState = quizStatesRepository.findById(getNextIdForChatId("quiz_states_data", chatId) - 1).get();

                        quizState.setAnswerId(id);

                        quizStatesRepository.save(quizState);

                        deleteMessage(chatId, messageId);

                        sendQuizPassingQuestionMessage(chatId, messageId);
                    }
                }
            }

            else {
                sendMessage(chatId, NO_CHANNEL_FOLLOW_TEXT);
            }
        }
    }

    private void start(long chatId) {
        if (isOwner(chatId)) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText("Здраствуйте, владелец " + usersRepository.findById(chatId).get().getFirstName());

            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);

            List<KeyboardRow> keyboardRows = new ArrayList<>();

            keyboardRows.add(createKeyboardRow(QUIZ_CREATE_KEYBOARD, QUIZ_UPDATE_KEYBOARD));
            keyboardRows.add(createKeyboardRow(SHOW_USERS_LEADERBOARD_KEYBOARD, SHOW_STATISTIC_KEYBOARD));
            keyboardRows.add(createKeyboardRow(USER_START_QUIZ_KEYBOARD));

            replyKeyboardMarkup.setKeyboard(keyboardRows);
            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            executeFunction(sendMessage);
        }

        else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText("Здраствуйте, " + usersRepository.findById(chatId).get().getFirstName());

            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);

            List<KeyboardRow> keyboardRows = new ArrayList<>();

            keyboardRows.add(createKeyboardRow(USER_START_QUIZ_KEYBOARD));

            replyKeyboardMarkup.setKeyboard(keyboardRows);
            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            executeFunction(sendMessage);
        }
    }

    private void registration(Message message) {
        long chatId = message.getChatId();
        Chat chat = message.getChat();

        User user = new User();

        user.setChatId(chatId);
        user.setUserName(chat.getUserName());
        user.setFirstName(chat.getFirstName());
        user.setLastName(chat.getLastName());
        user.setBetboomId(Long.valueOf(message.getText()));
        user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

        usersRepository.save(user);

        start(chatId);
    }

    private void sendRegistrationMessage(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton button = new InlineKeyboardButton();

        button.setText(BETBOOM_ACCOUNT_REGISTRATION_BUTTON);
        button.setUrl(config.getBetboomRegistrationURL());

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);

        markup.setKeyboard(keyboard);

        String text = "Привет! Чтоб начать проходить квиз " + bold("введи ID игрового счёта в BetBoom") +
                ", а если у тебя нет аккаунта — зарегистрируйся";

        sendPhoto(chatId, config.getRegistrationImagePath(), text, markup);
    }

    private void sendQwizStartCreateQuestionMessage(long chatId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createInlineKeyboardRow(CANCEL_BUTTON, YES_BUTTON));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        String text = "Создавая новый квиз, вы удаляете предыдущий. Продолжить?";

        sendMessage(chatId, text, markup);
    }

    private void sendQwizContinueCreateQuestionMessage(long chatId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createInlineKeyboardRow(NO_BUTTON, YES_BUTTON));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        String text = "Продолжить?";

        sendMessage(chatId, text, markup);
    }

    private void sendQwizUpdateQuestionMessage(long chatId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createInlineKeyboardRow(QUIZ_UPDATE_TIME_LIMIT_BUTTON, QUIZ_UPDATE_PRIZE_BUTTON));
        keyboard.add(createInlineKeyboardRow(CANCEL_BUTTON));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        String text = "Что вы хотите обновить";

        sendMessage(chatId, text, markup);
    }

    private void sendUserStartQuizQuestionMessage(long chatId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createInlineKeyboardRow(CANCEL_BUTTON, YES_BUTTON));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        String text = "Отвечайте правильно и быстро. Тогда вы сможете побороться за призы. Начинаем?";

        sendMessage(chatId, text, markup);
    }

    private void sendQuizPassingQuestionMessage(long chatId, int messageId) {
        QuizSetting quizSetting = quizSettingsRepository.findById(1).get();

        if (quizSettingsRepository.count() > 0 && (!isCurrentTimeLower(quizSetting.getTimeLimit()) || quizSetting.isWinnersNotification())) {
            sendMessage(chatId, "Квиз больше нельзя пройти");

            for (QuizState quizState : quizStatesRepository.findByColumn("chatId", chatId)) {
                quizStatesRepository.delete(quizState);
            }

            deleteState(chatId);
        }

        else {
            Question question = null;

            QuizState quizState = new QuizState();

            if (!quizStatesRepository.existsByChatId(chatId)) {
                question = questionsRepository.findById(1).get();
                quizState.setStartAt(new Timestamp(System.currentTimeMillis()));
            }

            else if (getNextIdForChatId("quiz_states_data", chatId) <= questionsRepository.count()) {
                question = questionsRepository.findById(getNextIdForChatId("quiz_states_data", chatId)).get();
            }

            else {
                Timestamp finishAt = new Timestamp(System.currentTimeMillis());

                addAnswerResultDatabase(chatId, finishAt);

                UserResult userResult = userResultsRepository.findById(chatId).get();

                sendMessage(chatId, String.format("Вы прошли квиз за %s и ответили правильно на %d/%d вопросов" +
                                "\n\n" + "Ожидайте сообщение с результатами %s",
                        userResult.getTimeSpent(), userResult.getResult(),
                        questionsRepository.count(), convertQuizLimitTimeToString(quizSetting.getTimeLimit())));

                deleteState(chatId);
                return;
            }

            List<AnswerOption> answerOptions = answerOptionsRepository.findByColumn("questionId", question.getId());

            InlineKeyboardMarkup markup = createAnswerMarkup(answerOptions);

            if (question.getImagePath() != null) {
                sendPhoto(chatId, question.getImagePath(), question.getText(), markup);
            }

            else {
                sendMessage(chatId, question.getText(), markup);
            }

            quizState.setChatId(chatId);
            quizState.setQuestionId(question.getId());

            quizStatesRepository.save(quizState);
        }
    }

    private void addQuestionDatabase(Message message) throws TelegramApiException, MalformedURLException {
        long chatId = message.getChatId();

        String text = null;
        String imagePath = null;

        if (message.hasPhoto()) {
            text = message.getCaption();
            imagePath = config.getQuestionImagesPath() + saveQuestionImage(message);
        }

        else {
            text = message.getText();
            imagePath = null;
        }

        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException();
        }

        String[] lines = text.split("\\n");
        StringBuilder questionTextBuilder = new StringBuilder();
        int answerStartIndex = 0;

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].matches("\\d+\\) .+")) {
                answerStartIndex = i;
                break;
            }

            questionTextBuilder.append(lines[i]).append("\n");
        }

        String questionText = questionTextBuilder.toString().trim();

        Question question = new Question();

        question.setText(questionText.isEmpty() ? null : questionText);
        question.setImagePath(imagePath);
        questionsRepository.save(question);

        for (int i = answerStartIndex; i < lines.length; i++) {
            String line = lines[i];

            if (line.matches("\\d+\\) .+")) {
                AnswerOption answerOption = new AnswerOption();

                answerOption.setQuestionId(question.getId());

                answerOption.setText(line.replaceAll("\\d+\\) ", "").replace(" +", "").trim());
                answerOption.setCorrect(line.contains("+"));

                answerOptionsRepository.save(answerOption);
            }
        }
    }

    private void addQuizTimeLimitDatabase(long chatId, String text) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(text, formatter);

        ZonedDateTime zonedDateTimeMSK = dateTime.atZone(ZoneId.of("Europe/Moscow"));
        ZonedDateTime zonedDateTimeUTC = zonedDateTimeMSK.withZoneSameInstant(ZoneId.of("UTC"));

        Timestamp timeLimit = Timestamp.from(zonedDateTimeUTC.toInstant());

        QuizSetting quizSetting = new QuizSetting();

        quizSetting.setTimeLimit(timeLimit);

        quizSettingsRepository.save(quizSetting);
    }

    private void addQuizPrizeDatabase(long chatId, String text) {
        String[] lines = text.split("\n");

        for (String line : lines) {
            String cleanedLine = line.trim();

            if (!cleanedLine.isEmpty()) {
                cleanedLine = cleanedLine.replaceAll("^\\d+\\)", "").trim();

                QuizPrize quizPrize = new QuizPrize();

                quizPrize.setPrize(cleanedLine);

                quizPrizesRepository.save(quizPrize);
            }
        }
    }

    private void updateQuizTimeLimitDatabase(long chatId, String text) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(text, formatter);

        ZonedDateTime zonedDateTimeMSK = dateTime.atZone(ZoneId.of("Europe/Moscow"));
        ZonedDateTime zonedDateTimeUTC = zonedDateTimeMSK.withZoneSameInstant(ZoneId.of("UTC"));

        Timestamp timeLimit = Timestamp.from(zonedDateTimeUTC.toInstant());

        QuizSetting quizSetting = quizSettingsRepository.findById(1).orElseThrow();

        quizSetting.setTimeLimit(timeLimit);

        quizSettingsRepository.save(quizSetting);
    }

    private void updateQuizPrizeDatabase(long chatId, String text) {
        for (QuizPrize quizPrize : quizPrizesRepository.findAll()) {
            quizPrizesRepository.delete(quizPrize);
        }

        String[] lines = text.split("\n");

        for (String line : lines) {
            String cleanedLine = line.trim();

            if (!cleanedLine.isEmpty()) {
                cleanedLine = cleanedLine.replaceAll("^\\d+\\)", "").trim();

                QuizPrize quizPrize = new QuizPrize();

                quizPrize.setPrize(cleanedLine);

                quizPrizesRepository.save(quizPrize);
            }
        }
    }

    private void addAnswerResultDatabase(long chatId, Timestamp finishAt) {
        List<QuizState> quizStates = quizStatesRepository.findByColumn("chatId", chatId);

        Timestamp startAt = quizStates.get(0).getStartAt();

        int correctCount = 0;

        for (QuizState quizState : quizStates) {
            AnswerOption correctAnswer = answerOptionsRepository.findCorrectAnswerByQuestionId(quizState.getQuestionId());
            if (correctAnswer != null && correctAnswer.getId().equals(quizState.getAnswerId())) {
                correctCount++;
            }
            quizStatesRepository.delete(quizState);
        }

        UserResult userResult = new UserResult();
        userResult.setChatId(chatId);
        userResult.setResult(correctCount);
        userResult.setRegisteredAt(finishAt);

        Instant startInstant = startAt.toInstant();
        Instant finishInstant = finishAt.toInstant();

        Duration duration = Duration.between(startInstant, finishInstant);

        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds() % 60;
        long milliseconds = duration.toMillis() % 1000;

        String time = String.format("%d:%02d:%02d", minutes, seconds, milliseconds / 10);

        userResult.setTimeSpent(time);

        userResultsRepository.save(userResult);
    }

    private String saveQuestionImage(Message message) throws TelegramApiException, MalformedURLException {
        String iconFileName = "image_" + System.currentTimeMillis() + ".jpg";
        String iconPath = config.getQuestionImagesPath() + iconFileName;

        String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
        File file = this.execute(new GetFile(fileId));
        String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();

        try (InputStream in = new URL(fileUrl).openStream()) {
            Files.copy(in, Path.of(iconPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return iconFileName;
    }

    private void deleteQuiz() throws IOException {
        questionsRepository.deleteAll();
        answerOptionsRepository.deleteAll();
        quizStatesRepository.deleteAll();
        userResultsRepository.deleteAll();
        quizSettingsRepository.deleteAll();
        quizPrizesRepository.deleteAll();

        Files.walk(Paths.get(config.getQuestionImagesPath()))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void setState(long chatId, ActionType action) {
        if (!differentStatesRepository.existsById(chatId)) {
            DifferentState differentState = new DifferentState();
            differentState.setChatId(chatId);

            differentState.setState(action.getCode());
            differentStatesRepository.save(differentState);
        }

        else {
            DifferentState differentState = differentStatesRepository.findById(chatId).get();
            differentState.setState(action.getCode());
            differentStatesRepository.save(differentState);
        }
    }

    private void deleteState(long chatId) {
        differentStatesRepository.deleteById(chatId);
    }

    private void checkQuizTimeLimitEnd() {
        if (quizPrizesRepository.existsById(1) && userResultsRepository.count() > 0) {
            QuizSetting quizSetting = quizSettingsRepository.findById(1).get();

            if (!isCurrentTimeLower(quizSetting.getTimeLimit()) && !quizSetting.isWinnersNotification()) {
                List<UserResult> leaderboard = userResultsRepository.findLeaderboardUserResults();
                List<QuizPrize> quizPrizes = (List<QuizPrize>) quizPrizesRepository.findAll();

                StringBuilder quizWinnersNotificationMessage = new StringBuilder(boldAndUnderline("ПОБЕДИТЕЛИ КВИЗА\n\n"));

                int prizeIndex = 0;
                for (UserResult userResult : leaderboard) {
                    if (prizeIndex < quizPrizes.size()) {
                        QuizPrize quizPrize = quizPrizes.get(prizeIndex);

                        quizWinnersNotificationMessage.append(prizeIndex + 1).append(") @")
                                .append(usersRepository.findById(userResult.getChatId()).get().getUserName())
                                .append(" - ").append(quizPrize.getPrize()).append("\n");

                        prizeIndex++;
                    }
                }

                for (User user : usersRepository.findAll()) {
                    sendMessage(user.getChatId(), quizWinnersNotificationMessage.toString());
                }

                quizSetting.setWinnersNotification(true);

                quizSettingsRepository.save(quizSetting);
            }
        }
    }

    private String convertQuizLimitTimeToString(Timestamp timestamp) {
        ZonedDateTime zonedDateTime = timestamp.toInstant().atZone(ZoneId.of("Europe/Moscow"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy в H:mm");

        return zonedDateTime.format(formatter);
    }

    private boolean isOwner(long chatId) {
        return config.getOwnerChatId() == chatId;
    }

    public boolean isUserInChannel(long chatId) {
        if (config.isCheckChannelChatId()) {
            try {
                GetChatMember getChatMember = new GetChatMember();

                getChatMember.setChatId(String.valueOf(config.getChannelChatId()));
                getChatMember.setUserId(chatId);

                ChatMember chatMember = execute(getChatMember);

                String status = chatMember.getStatus();
                return !status.equals("left") && !status.equals("kicked");
            } catch (TelegramApiException e) {
                e.printStackTrace();
                return false;
            }
        }

        else {
            return true;
        }
    }

    private boolean isUniqueBetboomId(String text) {
        return !usersRepository.existsByColumn("betboomId", Long.parseLong(text));
    }

    private boolean isCurrentTimeLower(Timestamp timestamp) {
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime inputTime = timestamp.toInstant().atZone(ZoneId.of("UTC"));

        return currentTime.isBefore(inputTime);
    }

    private boolean isSameDay(Timestamp timestamp, LocalDateTime now) {
        ZoneId mskZone = ZoneId.of("Europe/Moscow");

        LocalDateTime resultDate = timestamp.toInstant().atZone(mskZone).toLocalDateTime();
        LocalDateTime nowInMsk = now.atZone(mskZone).toLocalDateTime();

        return resultDate.toLocalDate().isEqual(nowInMsk.toLocalDate());
    }

    private boolean isSameWeek(Timestamp timestamp, LocalDateTime now) {
        ZoneId mskZone = ZoneId.of("Europe/Moscow");

        LocalDateTime resultDate = timestamp.toInstant().atZone(mskZone).toLocalDateTime();
        LocalDateTime nowInMsk = now.atZone(mskZone).toLocalDateTime();

        int resultWeek = resultDate.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
        int nowWeek = nowInMsk.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

        return resultDate.getYear() == nowInMsk.getYear() && resultWeek == nowWeek;
    }

    private boolean isValidQuizQuestionFormat(Message message) {
        String text = null;

        if (message.hasPhoto()) {
            text = message.getCaption();
        }

        else {
            text = message.getText();
        }

        if (text == null || text.isEmpty()) {
            return false;
        }

        String[] lines = text.split("\\n");

        boolean isTextMessage = !message.hasPhoto();

        boolean hasQuestion = false;
        boolean hasCorrectAnswer = false;
        int expectedNumber = 1;

        for (String line : lines) {
            line = line.trim();

            if (line.matches("\\d+\\) .+")) {
                String numberPart = line.split("\\)")[0];
                try {
                    int number = Integer.parseInt(numberPart);

                    if (number != expectedNumber) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }

                if (line.contains("+")) {
                    if (hasCorrectAnswer) {
                        return false;
                    }
                    hasCorrectAnswer = true;
                }

                expectedNumber++;
            }

            else if (isTextMessage && !hasQuestion && line.length() > 0) {
                hasQuestion = true;
            }
        }

        if (isTextMessage && !hasQuestion) {
            return false;
        }

        if (!isTextMessage && !hasQuestion) {
            return true;
        }

        return hasCorrectAnswer;
    }

    private boolean isValidQuizDateFormat(String text) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        try {
            LocalDateTime.parse(text, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isValidQuizPrizeFormat(String text) {
        String[] lines = text.split("\n");

        Pattern pattern = Pattern.compile("^\\d+\\)\\s.*$");

        int expectedNumber = 1;

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line.trim());

            if (!matcher.matches()) {
                return false;
            }

            String numberPart = line.trim().split("\\)")[0];
            try {
                int number = Integer.parseInt(numberPart);

                if (number != expectedNumber) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }

            expectedNumber++;
        }
        return true;
    }

    private boolean isNumeric(String text) {
        return text != null && text.matches("-?\\d+");
    }

    @Transactional
    private int getNextId(String tableName) {
        String selectMaxIdQuery = "SELECT COALESCE(MAX(id), 0) + 1 FROM %s;".formatted(tableName);
        return jdbcTemplate.queryForObject(selectMaxIdQuery, Integer.class);
    }

    @Transactional
    private int getNextIdForChatId(String tableName, long chatId) {
        String selectMaxIdQuery = """
        SELECT COALESCE(MAX(id), 0) + 1
        FROM %s
        WHERE chat_id = ?;
    """.formatted(tableName);
        return jdbcTemplate.queryForObject(selectMaxIdQuery, Integer.class, chatId);
    }

    private KeyboardRow createKeyboardRow(String... buttons) {
        KeyboardRow row = new KeyboardRow();
        row.addAll(Arrays.asList(buttons));
        return row;
    }

    private List<InlineKeyboardButton> createInlineKeyboardRow(String... buttons) {
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (String textAndCallback : buttons) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(textAndCallback);
            button.setCallbackData(textAndCallback);
            row.add(button);
        }
        return row;
    }

    private InlineKeyboardMarkup createAnswerMarkup(List<AnswerOption> answerOptions) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < answerOptions.size(); i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(createAnswerButton(answerOptions.get(i)));

            if (i + 1 < answerOptions.size()) {
                row.add(createAnswerButton(answerOptions.get(i + 1)));
            }

            rows.add(row);
        }

        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardButton createAnswerButton(AnswerOption answerOption) {
        InlineKeyboardButton button = new InlineKeyboardButton();

        button.setText(answerOption.getText());
        button.setCallbackData("ANSWER_OPTION_" + answerOption.getId() + "_BUTTON");
        return button;
    }

    private int extractNumberFromAnswerOptionButton(String callbackData) {
        Pattern pattern = Pattern.compile("ANSWER_OPTION_(\\d+)_BUTTON");
        Matcher matcher = pattern.matcher(callbackData);

        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }

        return 0;
    }

    private void sendMessage(long chatId, Object text, InlineKeyboardMarkup markup) {
        CompletableFuture.runAsync(() -> {
            SendMessage message = new SendMessage();

            message.enableHtml(true);

            message.setChatId(String.valueOf(chatId));

            message.setText(text.toString());
            message.setReplyMarkup(markup);

            try {
                executeFunction(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendMessage(long chatId, Object text) {
        CompletableFuture.runAsync(() -> {
            SendMessage message = new SendMessage();

            message.enableHtml(true);

            message.setChatId(String.valueOf(chatId));

            message.setText(text.toString());

            try {
                executeFunction(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendPhoto(long chatId, String photoPath, String caption, InlineKeyboardMarkup markup) {
        CompletableFuture.runAsync(() -> {
            SendPhoto sendPhoto = new SendPhoto();

            sendPhoto.setChatId(chatId);

            java.io.File photoFile = Paths.get(photoPath).toFile();

            if (!photoFile.exists()) {
                return;
            }

            sendPhoto.setParseMode(ParseMode.HTML);
            sendPhoto.setPhoto(new InputFile(photoFile));
            sendPhoto.setCaption(caption);
            sendPhoto.setReplyMarkup(markup);

            try {
                executeFunction(sendPhoto);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendPhoto(long chatId, String photoPath, String caption) {
        CompletableFuture.runAsync(() -> {
            SendPhoto sendPhoto = new SendPhoto();

            sendPhoto.setChatId(chatId);

            java.io.File photoFile = Paths.get(photoPath).toFile();

            if (!photoFile.exists()) {
                return;
            }

            sendPhoto.setParseMode(ParseMode.HTML);
            sendPhoto.setPhoto(new InputFile(photoFile));
            sendPhoto.setCaption(caption);

            try {
                executeFunction(sendPhoto);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void editMessageText(long chatId, int messageId, Object text, InlineKeyboardMarkup markup) {
        CompletableFuture.runAsync(() -> {
            EditMessageText message = new EditMessageText();

            message.enableHtml(true);

            message.setChatId(String.valueOf(chatId));

            message.setText((String) text);
            message.setReplyMarkup(markup);
            message.setMessageId(messageId);

            try {
                executeFunction(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void editMessageText(long chatId, int messageId, Object text) {
        CompletableFuture.runAsync(() -> {
            EditMessageText message = new EditMessageText();

            message.enableHtml(true);

            message.setChatId(String.valueOf(chatId));

            message.setText(text.toString());
            message.setMessageId(messageId);

            try {
                executeFunction(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void deleteMessage(long chatId, int messageId) {
        CompletableFuture.runAsync(() -> {
            DeleteMessage message = new DeleteMessage();

            message.setChatId(String.valueOf(chatId));

            message.setMessageId(messageId);

            try {
                executeFunction(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private void executeFunction(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private void executeFunction(EditMessageText message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private void executeFunction(DeleteMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private void executeFunction(SendPhoto message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }

    private static String bold(String text) {
        return "<b>%s</b>".formatted(text);
    }

    private static String italic(String text) {
        return "<i>%s</i>".formatted(text);
    }

    private static String boldAndItalic(String text) {
        return "<b><i>%s</i></b>".formatted(text);
    }

    private static String boldAndUnderline(String text) {
        return "<b><u>%s</u></b>".formatted(text);
    }
}
