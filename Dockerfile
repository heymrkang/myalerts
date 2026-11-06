FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew \
    && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon \
    && cp $(find build/libs -name '*.jar' ! -name '*-plain.jar') /app/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/app.jar app.jar
RUN mkdir -p /app/data

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
