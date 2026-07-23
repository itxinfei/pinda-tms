FROM openjdk:8-jre-alpine

ENV JAVA_OPTS="-Xmx256m -Xms128m"

ARG JAR_FILE=target/pd-netty-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} /app.jar

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
CMD ["--spring.profiles.active=docker"]
