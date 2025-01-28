package mainFiles.database.tables.userResuit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "user_results_data")
public class UserResult {

    @Id
    @Column(name = "chat_id", columnDefinition = "BIGINT")
    private Long chatId;

    @Column(name = "result", columnDefinition = "INTEGER")
    private Integer result;

    @Column(name = "time_spent", columnDefinition = "VARCHAR(255)")
    private String timeSpent;

    @Column(name = "registered_at", columnDefinition = "TIMESTAMP")
    private Timestamp registeredAt;
}



