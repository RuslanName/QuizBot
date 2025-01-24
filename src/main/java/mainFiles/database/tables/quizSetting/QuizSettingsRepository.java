package mainFiles.database.tables.quizSetting;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository("QuizSettingsRepository")
public interface QuizSettingsRepository extends CrudRepository<QuizSetting, Integer> {
}



