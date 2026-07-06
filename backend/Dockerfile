# ---- 1) build: gradle로 JAR 생성 (JDK 필요) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# (a) 빌드 스크립트/래퍼 먼저 → 의존성 레이어 캐시 (src만 바뀌면 재사용)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# (b) 소스 복사 후 빌드. 테스트는 ci.yml에서 별도 → 이미지 빌드는 -x test
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# ---- 2) runtime: JRE만 담아 경량화 ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms128m -Xmx384m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
