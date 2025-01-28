package mainFiles.services;

import lombok.Getter;

@Getter
public enum ActionType {
    REGISTRATION(1),
    QUIZ_START_CREATE_QUESTION(2),
    QUIZ_START_CREATE_ANSWER(3),
    QUIZ_CREATE(4),
    QUIZ_ADD_TIME_LIMIT(5),
    QUIZ_ADD_PRIZE(6),
    QUIZ_CONTINUE_CREATE_QUESTION(7),
    QUIZ_UPDATE_QUESTION(8),
    QUIZ_UPDATE_TIME_LIMIT(9),
    QUIZ_UPDATE_PRIZE(10),
    USER_START_QUIZ_QUESTION(11),
    USER_PASS_QUIZ(12);

    private final int code;

    ActionType(int code) {
        this.code = code;
    }
}



