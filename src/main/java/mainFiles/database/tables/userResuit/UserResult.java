package mainFiles.database.tables.userResuit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_results_data")
public class UserResult {

    @Id
    @Column(name = "chat_id", columnDefinition = "BIGINT")
    private Long chatId;

    @Column(name = "result", columnDefinition = "VARCHAR(255)")
    private Integer result;

    @Column(name = "time", columnDefinition = "DOUBLE PRECISION")
    private Double time;
}



