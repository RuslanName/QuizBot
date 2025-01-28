package mainFiles.database.tables.userPrize;

import mainFiles.database.utils.customRepositoryMethods.CustomRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository("quizPrizesRepository")
public interface QuizPrizesRepository extends CrudRepository<QuizPrize, Integer>, CustomRepository<QuizPrize> {
}


