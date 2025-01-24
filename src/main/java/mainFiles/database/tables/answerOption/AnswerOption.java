package mainFiles.database.tables.answerOption;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "answer_options_data")
public class AnswerOption {

    @Id
    @Column(name = "id", columnDefinition = "INTEGER")
    private Integer id;

    @Column(name = "question_id", columnDefinition = "INTEGER")
    private Integer questionId;

    @Column(name = "text", columnDefinition = "VARCHAR(255)")
    private String text;

    @Column(name = "correct", columnDefinition = "BOOLEAN")
    private Boolean correct;
}
