package mainFiles.database.tables.differentState;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "different_states_data")
public class DifferentState {

    @Id
    @Column(name = "chat_id", columnDefinition = "BIGINT")
    private Long chatId;

    @Column(name = "state", columnDefinition = "INTEGER")
    private Integer state;
}