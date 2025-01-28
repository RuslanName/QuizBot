package mainFiles.database.tables.answerOption;

import mainFiles.database.utils.customRepositoryMethods.CustomRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("answerOptionsRepository")
public interface AnswerOptionsRepository extends CrudRepository<AnswerOption, Integer>, CustomRepository<AnswerOption> {

    @Query("SELECT a FROM AnswerOption a WHERE a.questionId = :questionId AND a.correct = true")
    AnswerOption findCorrectAnswerByQuestionId(Integer questionId);
}

