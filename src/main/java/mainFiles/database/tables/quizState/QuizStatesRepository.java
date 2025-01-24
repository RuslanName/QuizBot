package mainFiles.database.tables.quizState;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("QuizStatesRepository")
public interface QuizStatesRepository extends CrudRepository<QuizState, Integer> {
    @Query("SELECT count(q) > 0 FROM QuizState q WHERE q.chatId = :chatId")
    boolean existsByChatId(Long chatId);
    List<QuizState> findByChatId(Long chatId);
}
