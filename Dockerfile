FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu

ARG JAR_FILE
ENV JAVA_OPTS=""

WORKDIR /app
COPY ${JAR_FILE} app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
