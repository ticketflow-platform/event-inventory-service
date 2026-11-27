FROM gradle:9.7.1-jdk26-alpine AS build
USER gradle
WORKDIR /home/gradle

COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts gradle.properties ./
COPY --chown=gradle:gradle src/main ./src/main

RUN gradle --no-daemon bootJar \
    && cp build/libs/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:26-jre-alpine AS runtime

RUN apk add --no-cache curl \
    && addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app -h /nonexistent -s /sbin/nologin app

WORKDIR /application
COPY --from=build /home/gradle/extracted/dependencies/ ./
COPY --from=build /home/gradle/extracted/spring-boot-loader/ ./
COPY --from=build /home/gradle/extracted/snapshot-dependencies/ ./
COPY --from=build /home/gradle/extracted/application/ ./

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl --fail --silent --show-error --max-time 2 --output /dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-jar", "application.jar"]
