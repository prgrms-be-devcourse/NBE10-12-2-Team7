FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE=build/libs/dongnemarket-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms128m -Xmx384m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]