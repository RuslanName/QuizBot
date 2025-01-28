package mainFiles.database.utils.customIdGenerator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class CustomIdGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    public <T> Integer generateIdForEntity(Class<T> entityClass) {
        try {
            Field idField = getIdField(entityClass);
            String idFieldName = idField.getName();

            String query = "SELECT MAX(e." + idFieldName + ") FROM " + entityClass.getSimpleName() + " e";
            Integer maxId = (Integer) entityManager.createQuery(query).getSingleResult();

            return (maxId == null ? 1 : maxId + 1);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public Field getIdField(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new IllegalStateException();
    }
}
