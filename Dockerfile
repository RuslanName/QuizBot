# Сборка Java-приложения
FROM docker.io/library/maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . /app
RUN mvn clean package -DskipTests

# Финальный образ
FROM openjdk:17-alpine

# Копирование JAR файла приложения
COPY --from=build /app/target/TelegramWebApp-1.0.0-RELEASE.jar /app/

# Установка рабочей директории
WORKDIR /app

# Запуск Java-приложения
CMD ["java", "-jar", "TelegramWebApp-1.0.0-RELEASE.jar"]
