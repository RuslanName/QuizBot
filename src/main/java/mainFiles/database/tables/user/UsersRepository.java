package mainFiles.database.tables.user;

import mainFiles.database.utils.customRepositoryMethods.CustomRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository("usersRepository")
public interface UsersRepository extends CrudRepository<User, Long>, CustomRepository<User> {
}

