package mainFiles.database.utils.customRepositoryMethods;

import java.util.List;

public interface CustomRepository<T> {
    List<T> findByColumn(String columnName, Object value);

    boolean existsByColumn(String columnName, Object value);

    void deleteByColumn(String columnName, Object value);

    List<T> findByIdAndColumn(Object id, String columnName, Object value);

    boolean existsByIdAndColumn(Object id, String columnName, Object value);
}
