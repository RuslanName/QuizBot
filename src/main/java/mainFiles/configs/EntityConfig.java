package mainFiles.configs;

import jakarta.annotation.PostConstruct;
import mainFiles.database.utils.customIdGenerator.CustomIdGenerator;
import mainFiles.database.utils.customIdGenerator.AbstractEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EntityConfig {

    @Autowired
    private CustomIdGenerator idGenerator;

    @PostConstruct
    public void init() {
        AbstractEntity.setIdGenerator(idGenerator);
    }
}
