package mainFiles.database.tables.quizSetting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mainFiles.database.utils.customIdGenerator.AbstractEntity;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "quiz_settings_data")
public class QuizSetting extends AbstractEntity<QuizSetting> {
    @Id
    @Column(name = "id", columnDefinition = "INTEGER")
    private Integer id;

    @Column(name = "timeLimit", columnDefinition = "TIMESTAMP")
    private Timestamp timeLimit;
}
