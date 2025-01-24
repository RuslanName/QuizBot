package mainFiles.database.tables.userResuit;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("userResultsRepository")
public interface UserResultsRepository extends CrudRepository<UserResult, Long> {
    @Query("SELECT u FROM UserResult u ORDER BY u.result DESC, u.time ASC")
    List<UserResult> findLeaderboardUserResults();
}
