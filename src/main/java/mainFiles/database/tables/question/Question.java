package mainFiles.database.tables.question;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;
import mainFiles.database.utils.customIdGenerator.AbstractEntity;

@Getter
@Setter
@Entity
@Table(name = "questions_data")
public class Question extends AbstractEntity<Question> {

    @Id
    @Column(name = "id", columnDefinition = "INTEGER")
    private Integer id;

    @Column(name = "text", columnDefinition = "VARCHAR(255)")
    private String text;

    @Column(name = "icon_path", columnDefinition = "VARCHAR(255)")
    private String iconPath;
}
