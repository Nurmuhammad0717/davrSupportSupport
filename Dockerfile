FROM gradle:7.5.1-jdk11-alpine AS BUILD_STAGE
COPY --chown=gradle:gradle . /home/gradle
RUN gradle build || return 1

FROM openjdk:11.0.11-jre
ENV ARTIFACT_NAME=support-0.0.1-SNAPSHOT.jar
ENV APP_HOME=/app
ARG SPRING_PROFILES_ACTIVE
ENV SPRING_PROFILES_ACTIVE $SPRING_PROFILES_ACTIVE
COPY --from=BUILD_STAGE /home/gradle/build/libs/$ARTIFACT_NAME $APP_HOME/
WORKDIR $APP_HOME
RUN curl -o elastic-apm-agent.jar https://nexus.davrbank.uz/repository/maven-central/co/elastic/apm/elastic-apm-agent/1.49.0/elastic-apm-agent-1.49.0.jar
RUN groupadd -r -g 1000 user && useradd -r -g user -u 1000 user
RUN chown -R user:user /app
USER user
ENV TZ=Asia/Tashkent
ENTRYPOINT exec java -jar ${ARTIFACT_NAME}
