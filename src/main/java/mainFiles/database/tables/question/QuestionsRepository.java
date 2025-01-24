package mainFiles.database.tables.question;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository("questionsRepository")
public interface QuestionsRepository extends CrudRepository<Question, Integer> {
}

