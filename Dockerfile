# ---------- 1단계: 빌드 ----------
FROM gradle:8.9-jdk17 AS builder
WORKDIR /app

# Gradle 캐시 최적화
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon || return 0

# 소스 복사 후 빌드
COPY . .
RUN ./gradlew clean bootJar --no-daemon

# ---------- 2단계: 실행 ----------
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 빌드 산출물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java","-jar","app.jar"]
