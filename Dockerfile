# Сборка Java-приложения
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . /app
RUN mvn clean package -DskipTests

# Финальный образ
FROM openjdk:17-jdk-slim

# Копирование JAR файла приложения
COPY --from=build /app/target/TelegramWebApp-1.0.0-RELEASE.jar /app/TelegramWebApp.jar

# Открытие порта для приложения
EXPOSE 8080

# Запуск Java-приложения
CMD ["java", "-jar", "/app/TelegramWebApp.jar"]
