FROM amazoncorretto:17

# 컨테이너 기본 시간대를 한국으로 고정. amazoncorretto 기본값은 UTC 라
# LocalDateTime.now() (JPA Auditing createdAt/updatedAt) 가 9시간 밀려 저장됨.
ENV TZ=Asia/Seoul

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app.jar
COPY env/prod.env /env/prod.env

EXPOSE 8080
# -Duser.timezone 으로 JVM 기본 시간대까지 KST 로 못박음 (TZ 미반영 환경 대비).
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app.jar"]