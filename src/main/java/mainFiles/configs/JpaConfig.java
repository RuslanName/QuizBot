package mainFiles.configs;

import mainFiles.database.utils.customRepositoryMethods.CustomRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
        basePackages = "mainFiles.database.tables",
        repositoryBaseClass = CustomRepositoryImpl.class
)
public class JpaConfig {
}


