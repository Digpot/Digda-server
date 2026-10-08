FROM amazoncorretto:17

# 컨테이너 기본 시간대를 한국으로 고정. amazoncorretto 기본값은 UTC 라
# LocalDateTime.now() (JPA Auditing createdAt/updatedAt) 가 9시간 밀려 저장됨.
ENV TZ=Asia/Seoul

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app.jar
# 운영 비밀값(env/prod.env)은 이미지에 굽지 않는다. 이미지는 DockerHub 에 올라가므로
# 이미지 안에 있으면 pull 한 누구나 DB 비밀번호·JWT 시크릿·AWS 키를 꺼낼 수 있다.
# 서버의 /home/ubuntu/digda/env/prod.env 를 docker-compose 가 /env/prod.env 로 읽기 전용 마운트한다.

EXPOSE 8080
# 908MB 박스에 MySQL 이 같이 올라가므로 JVM 이 알아서 크게 잡지 못하게 못박는다.
#  -Xmx240m       : 컨테이너 기본값(RAM 의 25% ≈ 227m)과 사실상 같은 수준. 낮추는 게 아니라
#                   "MySQL 이 메모리를 가져가도 JVM 이 따라 늘지 않게" 고정하는 목적.
#  MaxMetaspace   : 상한이 없으면 계속 자라므로 못박되, Spring Boot + Hibernate + Jackson 은
#                   런타임 클래스 생성까지 포함해 150~200m 를 쓴다. 128m 은 첫 요청에서
#                   OutOfMemoryError: Metaspace 로 죽어 256m 로 올렸다.
#  UseSerialGC    : 1GB/2vCPU 급에서 G1 은 region 메타데이터로 수십 MB 를 더 쓴다.
#  MaxDirectMemory: S3 업로드·WebSocket 의 다이렉트 버퍼 상한.
# -Duser.timezone 으로 JVM 기본 시간대까지 KST 로 못박음 (TZ 미반영 환경 대비).
ENTRYPOINT ["java", \
  "-Xms160m", \
  "-Xmx240m", \
  "-XX:MaxMetaspaceSize=256m", \
  "-XX:+UseSerialGC", \
  "-XX:MaxDirectMemorySize=48m", \
  "-Duser.timezone=Asia/Seoul", \
  "-jar", "/app.jar"]
