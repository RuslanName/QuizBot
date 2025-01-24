package mainFiles.services;

import lombok.Getter;

@Getter
public enum ActionType {
    REGISTRATION(1),
    QUIZ_START_CREATE_QUESTION(2),
    QUIZ_START_CREATE_ANSWER(3),
    QUIZ_CREATE(3),
    QUIZ_ADD_TIME_LIMIT(4),
    QUIZ_UPDATE_TIME_LIMIT(5),
    QUIZ_CONTINUE_CREATE_QUESTION(6),
    USER_START_QUIZ_QUESTION(7),
    USER_PASS_QUIZ(8);

    private final int code;

    ActionType(int code) {
        this.code = code;
    }
}



