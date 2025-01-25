package mainFiles.database.tables.differentState;

import mainFiles.database.utils.customRepositoryMethods.CustomRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository("differentStatesRepository")
public interface DifferentStatesRepository extends CrudRepository<DifferentState, Long>, CustomRepository<DifferentState> {
}
