package mainFiles.database.tables.answerOption;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("answerOptionsRepository")
public interface AnswerOptionsRepository extends CrudRepository<AnswerOption, Integer> {
    @Query("SELECT a FROM AnswerOption a WHERE a.questionId = :questionId AND a.correct = true")
    AnswerOption findCorrectAnswerByQuestionId(Integer questionId);
    List<AnswerOption> findByQuestionId(Integer questionId);
}

