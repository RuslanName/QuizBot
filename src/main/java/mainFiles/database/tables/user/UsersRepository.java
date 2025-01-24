package mainFiles.database.tables.user;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository("usersRepository")
public interface UsersRepository extends CrudRepository<User, Long> {
    @Query("SELECT count(u) > 0 FROM User u WHERE u.betboomId = :betboomId")
    boolean existsByBetboomId(Long betboomId);
}
