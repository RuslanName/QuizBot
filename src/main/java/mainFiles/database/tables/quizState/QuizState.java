package mainFiles.database.tables.quizState;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "quiz_states_data")
public class QuizState {

    @Id
    @Column(name = "id", columnDefinition = "INTEGER")
    private Integer id;

    @Column(name = "chat_id", columnDefinition = "BIGINT")
    private Long chatId;

    @Column(name = "question_id", columnDefinition = "INTEGER")
    private Integer questionId;

    @Column(name = "answer_id", columnDefinition = "INTEGER")
    private Integer answerId;

    @Column(name = "start_at", columnDefinition = "TIMESTAMP")
    private Timestamp startAt;
}