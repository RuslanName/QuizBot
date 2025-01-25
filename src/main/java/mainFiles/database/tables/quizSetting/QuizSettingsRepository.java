package mainFiles.database.tables.quizSetting;

import mainFiles.database.utils.customRepositoryMethods.CustomRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository("QuizSettingsRepository")
public interface QuizSettingsRepository extends CrudRepository<QuizSetting, Integer>, CustomRepository<QuizSetting> {
}



