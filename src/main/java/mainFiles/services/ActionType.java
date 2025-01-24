package mainFiles.services;

import lombok.Getter;

@Getter
public enum ActionType {
    REGISTRATION(1),
    QUIZ_START_CREATE_QUESTION(2),
    QUIZ_START_CREATE_ANSWER(3),
    QUIZ_CREATE(3),
    QUIZ_ADD_TIME_LIMIT(4),
    QUIZ_CONTINUE_CREATE_QUESTION(5),
    USER_START_QUIZ_QUESTION(6),
    USER_PASS_QUIZ(7);

    private final int code;

    ActionType(int code) {
        this.code = code;
    }
}



