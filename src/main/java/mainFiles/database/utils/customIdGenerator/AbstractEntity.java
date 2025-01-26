package mainFiles.database.utils.customIdGenerator;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import lombok.Setter;

import java.lang.reflect.Field;

@MappedSuperclass
public abstract class AbstractEntity<T> {

    @Setter
    @Transient
    private static CustomIdGenerator idGenerator;

    @PrePersist
    public void generateId() {
        if (idGenerator != null) {
            Class<?> entityClass = this.getClass();
            Field idField = idGenerator.getIdField(entityClass);

            try {
                idField.setAccessible(true);
                Object currentValue = idField.get(this);

                if (currentValue == null) {
                    Integer newId = idGenerator.generateIdForEntity(entityClass);
                    idField.set(this, newId);
                }
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }
    }
}
