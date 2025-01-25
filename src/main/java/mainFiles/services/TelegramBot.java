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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
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
    private UsersRepository usersRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    final BotConfig config;

    static final String QUIZ_CREATE_KEYBOARD = "Создать квиз";
    static final String QUIZ_UPDATE_TIME_LIMIT_KEYBOARD = "Изменить время";
    static final String SHOW_USERS_LEADERBOARD_KEYBOARD = "Список лидеров";
    static final String USER_START_QUIZ_KEYBOARD = "Начать прохождение квиза";

    static final String BETBOOM_ACCOUNT_REGISTRATION_BUTTON = "Регистрация в Betboom";
    static final String NO_BUTTON = "Нет";
    static final String YES_BUTTON = "Да";

    static final String NO_CHANNEL_FOLLOW_TEXT = "<b>Вы не подписаны на канал: https://t.me/roganov_hockey</b> \n" +
            "<i>Подпишитесь, и сможете пользоваться ботом</i>";

    static final String HELP_TEXT = boldAndUnderline("СПИСОК КОМАНД") + "\n\n" +
            "/start - запустить бота \n" +
            "/registration - зарегестрироваться \n" +
            "/help - показать информацию о возможностях бота \n";

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
                        sendMessage(chatId, HELP_TEXT);
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

                    else if (text.equals(QUIZ_UPDATE_TIME_LIMIT_KEYBOARD) && isOwner(chatId)) {
                        setState(chatId, ActionType.QUIZ_UPDATE_TIME_LIMIT);
                        sendMessage(chatId, "Введите ограничение времени для прохождения квиза");
                    }

                    else if (text.equals(SHOW_USERS_LEADERBOARD_KEYBOARD) && isOwner(chatId)) {
                        List<UserResult> leaderboardUserResults = userResultsRepository.findLeaderboardUserResults();

                        if (leaderboardUserResults.isEmpty()) {
                            sendMessage(chatId, "Список лидеров пуст");
                        }

                        else {
                            StringBuilder leaderboardMessage = new StringBuilder(boldAndUnderline("СПИСОК ЛИДЕРОВ\n\n"));

                            int i = 1;
                            for (UserResult userResult : leaderboardUserResults) {
                                User user = usersRepository.findById(userResult.getChatId()).orElseThrow();
                                leaderboardMessage.append(String.format("%d) @%s - Ответы: %d, Время: %.2f с. | Betboom ID: %d\n",
                                        i++, user.getUserName(), userResult.getResult(), userResult.getTime(), user.getBetboomId()));
                            }

                            sendMessage(chatId, leaderboardMessage.toString());
                        }
                    }

                    else if (text.equals(USER_START_QUIZ_KEYBOARD) && usersRepository.existsById(chatId)) {
                        if (userResultsRepository.existsById(chatId)) {
                            sendMessage(chatId, "Вы уже прошли квиз");
                        }

                        else {
                            Optional<QuizSetting> quizSetting = quizSettingsRepository.findById(1);

                            if (quizSetting.isPresent() && !isCurrentTimeLower(quizSetting.get().getTimeLimit())) {
                                sendMessage(chatId, "Квиз больше нельзя пройти");
                            }

                            else {
                                differentStatesRepository.findById(chatId)
                                        .map(differentState -> differentState.getState())
                                        .filter(state -> state == ActionType.USER_PASS_QUIZ.getCode())
                                        .ifPresentOrElse(
                                                state -> {
                                                    sendMessage(chatId, "Вы уже проходите квиз");
                                                },
                                                () -> {
                                                    setState(chatId, ActionType.USER_START_QUIZ_QUESTION);
                                                    sendUserStartQuizQuestionMessage(chatId);
                                                }
                                        );
                            }
                        }
                    }

                    else if (differentStatesRepository.existsById(chatId)) {
                        if (differentStatesRepository.findById(chatId).get().getState() == ActionType.REGISTRATION.getCode()) {
                            if (isNumeric(text)) {
                                if (isUniqueId(text)) {
                                    registration(message);
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
                            if (quizQuestionCorrectness(message)) {
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
                            if (isValidDateFormat(text)) {
                                addQuizTimeLimitDatabase(chatId, text);

                                sendMessage(chatId, "Создание квиза завершено");

                                for (User user : usersRepository.findAll()) {
                                    if (!isOwner(chatId)) {
                                        sendMessage(chatId, "Добавлен новый квиз");
                                    }
                                }
                            }

                            else {
                                sendMessage(chatId, "Время введено неправильно. Проверьте корректность ввода и введите снова");
                            }
                        }

                        else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.QUIZ_UPDATE_TIME_LIMIT.getCode()) {
                            if (isValidDateFormat(text)) {
                                updateQuizTimeLimitDatabase(chatId, text);

                                sendMessage(chatId, "Время обновлено");
                            }

                            else {
                                sendMessage(chatId, "Время введено неправильно. Проверьте корректность ввода и введите снова");
                            }
                        }
                    }
                }

                else if (message.hasPhoto()) {
                    if (differentStatesRepository.existsById(chatId)) {
                        if (differentStatesRepository.findById(chatId).get().getState() == ActionType.QUIZ_CREATE.getCode() && quizQuestionCorrectness(message)) {
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
                    if (callbackData.equals(NO_BUTTON)) {
                        differentStatesRepository.deleteById(chatId);
                        editMessageText(chatId, messageId, "Создание нового квиза отменено");
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

                else if (differentStatesRepository.findById(chatId).get().getState() == ActionType.USER_START_QUIZ_QUESTION.getCode()) {
                    if (callbackData.equals(NO_BUTTON)) {
                        differentStatesRepository.deleteById(chatId);
                        editMessageText(chatId, messageId, "Прохождение квиза отменено");
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

                        QuizState quizState = quizStatesRepository.findById(getNextIdForChatId("quiz_states_data", chatId) - 1).orElseThrow();

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
            sendMessage.setText("Здраствуйте, владелец");

            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);

            List<KeyboardRow> keyboardRows = new ArrayList<>();

            keyboardRows.add(createKeyboardRow(QUIZ_CREATE_KEYBOARD, QUIZ_UPDATE_TIME_LIMIT_KEYBOARD));
            keyboardRows.add(createKeyboardRow(SHOW_USERS_LEADERBOARD_KEYBOARD, USER_START_QUIZ_KEYBOARD));

            replyKeyboardMarkup.setKeyboard(keyboardRows);
            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            executeFunction(sendMessage);
        }

        else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText("Здраствуйте");

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

        String text = "Здравствуйте! Вам нужно зарегистрироваться. Введите свой Betboom ID";

        sendMessage(chatId, text, markup);
    }

    private void sendQwizStartCreateQuestionMessage(long chatId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createInlineKeyboardRow(NO_BUTTON, YES_BUTTON));

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

    private void sendUserStartQuizQuestionMessage(long chatId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createInlineKeyboardRow(NO_BUTTON, YES_BUTTON));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        String text = "В квизе " + questionsRepository.count() + " вопросов. " +
                "Ваша задача попытаться ответить как можно быстрее и правильнее. Ограничения по времени нет. Начать?";

        sendMessage(chatId, text, markup);
    }

    private void sendQuizPassingQuestionMessage(long chatId, int messageId) {
        Optional<QuizSetting> quizSetting = quizSettingsRepository.findById(1);

        if (quizSetting.isPresent() && !isCurrentTimeLower(quizSetting.get().getTimeLimit())) {
            sendMessage(chatId, "Квиз больше нельзя пройти");

            for (QuizState quizState : quizStatesRepository.findByChatId(chatId)) {
                quizStatesRepository.delete(quizState);
            }

            differentStatesRepository.deleteById(chatId);
        }

        else {
            Question question = null;

            QuizState quizState = new QuizState();

            if (!quizStatesRepository.existsByChatId(chatId)) {
                question = questionsRepository.findById(1).orElseThrow();
                quizState.setStartAt(new Timestamp(System.currentTimeMillis()));
            }

            else if (getNextIdForChatId("quiz_states_data", chatId) <= questionsRepository.count()) {
                question = questionsRepository.findById(getNextIdForChatId("quiz_states_data", chatId)).orElseThrow();
            }

            else {
                Timestamp finishAt = new Timestamp(System.currentTimeMillis());

                addAnswerResultDatabase(chatId, finishAt);

                UserResult userResult = userResultsRepository.findById(chatId).orElseThrow();

                sendMessage(chatId, String.format("Вы прошли квиз за %.2f с. У вас %d из %d правильных ответов",
                        userResult.getTime(), userResult.getResult(), questionsRepository.count()));

                differentStatesRepository.deleteById(chatId);
                return;
            }

            List<AnswerOption> answerOptions = answerOptionsRepository.findByQuestionId(question.getId());

            InlineKeyboardMarkup markup = createAnswerMarkup(answerOptions);

            if (question.getIconPath() != null) {
                sendPhoto(chatId, question.getIconPath(), question.getText(), markup);
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
        String iconPath = null;

        if (message.hasPhoto()) {
            text = message.getCaption();
            iconPath = config.getQuestionIconsPath() + saveQuestionIcon(message);
        }

        else {
            text = message.getText();
            iconPath = null;
        }

        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Сообщение не содержит текста для обработки.");
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
        question.setIconPath(iconPath);
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

        Timestamp timeLimit = Timestamp.valueOf(dateTime.atZone(ZoneId.systemDefault()).toLocalDateTime());

        QuizSetting quizSetting = new QuizSetting();

        quizSetting.setTimeLimit(timeLimit);

        quizSettingsRepository.save(quizSetting);

        differentStatesRepository.deleteById(chatId);
    }

    private void updateQuizTimeLimitDatabase(long chatId, String text) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(text, formatter);

        Timestamp timeLimit = Timestamp.valueOf(dateTime.atZone(ZoneId.systemDefault()).toLocalDateTime());

        QuizSetting quizSetting = quizSettingsRepository.findById(1).orElseThrow();

        quizSetting.setTimeLimit(timeLimit);

        quizSettingsRepository.save(quizSetting);

        differentStatesRepository.deleteById(chatId);
    }

    private void addAnswerResultDatabase(long chatId, Timestamp finishAt) {
        List<QuizState> quizStates = quizStatesRepository.findByChatId(chatId);

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

        Instant startInstant = startAt.toInstant();
        Instant finishInstant = finishAt.toInstant();

        Duration duration = Duration.between(startInstant, finishInstant);

        long seconds = duration.getSeconds();
        long milliseconds = duration.toMillis() % 1000;

        String time = String.format("%d.%02d", seconds, milliseconds);

        userResult.setTime(Double.valueOf(time));

        userResultsRepository.save(userResult);
    }

    private boolean quizQuestionCorrectness(Message message) {
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

        String questionPattern = "^(.*\\n)?(\\d+\\) .+\\n?)+$";
        Pattern pattern = Pattern.compile(questionPattern, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        if (!matcher.matches()) {
            return false;
        }

        String[] lines = text.split("\\n");
        boolean hasCorrectAnswer = false;

        for (String line : lines) {
            if (line.matches("\\d+\\) .+")) {
                if (line.contains("+")) {
                    if (hasCorrectAnswer) {
                        return false;
                    }
                    hasCorrectAnswer = true;
                }
            }
        }

        return hasCorrectAnswer;
    }

    private String saveQuestionIcon(Message message) throws TelegramApiException, MalformedURLException {
        String iconFileName = "icon_" + System.currentTimeMillis() + ".jpg";
        String iconPath = config.getQuestionIconsPath() + iconFileName;

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

        Files.walk(Paths.get(config.getQuestionIconsPath()))
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

    private boolean isUniqueId(String text) {
        return !usersRepository.existsByColumn("betboomId", Long.parseLong(text));
    }

    private boolean isCurrentTimeLower(Timestamp text) {
        ZoneId mskZone = ZoneId.of("Europe/Moscow");
        LocalDateTime currentDateTime = LocalDateTime.now(mskZone);

        LocalDateTime inputDateTime = text.toLocalDateTime();

        return currentDateTime.isBefore(inputDateTime);
    }

    private boolean isValidDateFormat(String text) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        try {
            LocalDateTime.parse(text, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public boolean isNumeric(String text) {
        return text != null && text.matches("-?\\d+");
    }

    @Transactional
    public int getNextId(String tableName) {
        String selectMaxIdQuery = "SELECT COALESCE(MAX(id), 0) + 1 FROM %s;".formatted(tableName);
        return jdbcTemplate.queryForObject(selectMaxIdQuery, Integer.class);
    }

    @Transactional
    public int getNextIdForChatId(String tableName, long chatId) {
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

    public static Integer extractNumberFromAnswerOptionButton(String callbackData) {
        Pattern pattern = Pattern.compile("ANSWER_OPTION_(\\d+)_BUTTON");
        Matcher matcher = pattern.matcher(callbackData);

        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }

        else {
            return null;
        }
    }

    private void sendMessage(long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.enableHtml(true);
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(markup);

        executeFunction(message);
    }

    private void sendMessage(long chatId, Object text) {
        SendMessage message = new SendMessage();
        message.enableHtml(true);
        message.setChatId(String.valueOf(chatId));
        message.setText(text.toString());

        executeFunction(message);
    }

    private void sendPhoto(long chatId, String photoPath, String caption, InlineKeyboardMarkup markup) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);

        java.io.File photoFile = Paths.get(photoPath).toFile();
        if (!photoFile.exists()) {
            System.err.println("Error: File not found at " + photoPath);
            return;
        }

        sendPhoto.setPhoto(new InputFile(photoFile));
        sendPhoto.setCaption(caption);
        sendPhoto.setReplyMarkup(markup);

        try {
            executeFunction(sendPhoto);
        } catch (Exception e) {
            System.err.println("Error: Unable to send photo");
            e.printStackTrace();
        }
    }

    private void editMessageText(long chatId, int messageId, Object text, InlineKeyboardMarkup markup) {
        EditMessageText message = new EditMessageText();
        message.enableHtml(true);
        message.setChatId(String.valueOf(chatId));
        message.setText(text.toString());
        message.setReplyMarkup(markup);
        message.setMessageId(messageId);

        executeFunction(message);
    }

    private void editMessageText(long chatId, int messageId, Object text) {
        EditMessageText message = new EditMessageText();
        message.enableHtml(true);
        message.setChatId(String.valueOf(chatId));
        message.setText(text.toString());
        message.setMessageId(messageId);

        executeFunction(message);
    }

    private void deleteMessage(long chatId, int messageId) {
        DeleteMessage message = new DeleteMessage();
        message.setChatId(String.valueOf(chatId));
        message.setMessageId(messageId);

        executeFunction(message);
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
