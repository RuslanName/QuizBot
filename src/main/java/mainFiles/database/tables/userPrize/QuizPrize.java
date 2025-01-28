package mainFiles.database.tables.userPrize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mainFiles.database.utils.customIdGenerator.AbstractEntity;

@Getter
@Setter
@Entity
@Table(name = "quiz_prizes_data")
public class QuizPrize extends AbstractEntity<QuizPrize> {

    @Id
    @Column(name = "id", columnDefinition = "INTEGER")
    private Integer id;

    @Column(name = "prize", columnDefinition = "VARCHAR(255)")
    private String prize;
}
