package mainFiles.database.tables.userResuit;

import mainFiles.database.utils.customRepositoryMethods.CustomRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("userResultsRepository")
public interface UserResultsRepository extends CrudRepository<UserResult, Long>, CustomRepository<UserResult> {
    @Query("SELECT u FROM UserResult u ORDER BY u.result DESC, u.timeSpent ASC")
    List<UserResult> findLeaderboardUserResults();
}
