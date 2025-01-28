package mainFiles.database.utils.customRepositoryMethods;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.util.List;

public class CustomRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> implements CustomRepository<T> {

    private final EntityManager entityManager;

    public CustomRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    public List<T> findByColumn(String columnName, Object value) {
        String queryString = "SELECT e FROM " + getDomainClass().getSimpleName() + " e WHERE e." + columnName + " = :value";
        var query = entityManager.createQuery(queryString, getDomainClass());
        query.setParameter("value", value);
        return query.getResultList();
    }

    @Override
    public boolean existsByColumn(String columnName, Object value) {
        String queryString = "SELECT COUNT(e) FROM " + getDomainClass().getSimpleName() + " e WHERE e." + columnName + " = :value";
        var query = entityManager.createQuery(queryString, Long.class);
        query.setParameter("value", value);
        return query.getSingleResult() > 0;
    }

    @Override
    public void deleteByColumn(String columnName, Object value) {
        String queryString = "DELETE FROM " + getDomainClass().getSimpleName() + " e WHERE e." + columnName + " = :value";
        var query = entityManager.createQuery(queryString);
        query.setParameter("value", value);
        query.executeUpdate();
    }

    @Override
    public boolean existsByIdAndColumn(Object id, String columnName, Object value) {
        String queryString = "SELECT COUNT(e) FROM " + getDomainClass().getSimpleName() + " e WHERE e.id = :id AND e." + columnName + " = :value";
        var query = entityManager.createQuery(queryString, Long.class);
        query.setParameter("id", id);
        query.setParameter("value", value);
        return query.getSingleResult() > 0;
    }

    @Override
    public List<T> findByIdAndColumn(Object id, String columnName, Object value) {
        String queryString = "SELECT e FROM " + getDomainClass().getSimpleName() + " e WHERE e.id = :id AND e." + columnName + " = :value";
        var query = entityManager.createQuery(queryString, getDomainClass());
        query.setParameter("id", id);
        query.setParameter("value", value);
        return query.getResultList();
    }
}
