package mainFiles.database.tables.question;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "questions_data")
public class Question {

    @Id
    @Column(name = "id", columnDefinition = "INTEGER")
    private Integer id;

    @Column(name = "text", columnDefinition = "VARCHAR(255)")
    private String text;

    @Column(name = "icon_path", columnDefinition = "VARCHAR(255)")
    private String iconPath;
}
