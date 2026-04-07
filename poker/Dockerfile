FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app/poker
COPY . /app/poker
RUN javac MainClass.java models/*.java game/*.java evaluation/*.java players/*.java ui/*.java

FROM tsl0922/ttyd:1.7.7 AS ttyd

FROM eclipse-temurin:17-jre-jammy

ENV APP_HOME=/app/poker
ENV PLAYER_COUNT=4
ENV MATCH_HANDS=5
ENV WEB_PORT=8080
ENV ALLOW_NO_AUTH=false

WORKDIR /app/poker

COPY . /app/poker
COPY --from=builder /app/poker/*.class /app/poker/
COPY --from=builder /app/poker/models/*.class /app/poker/models/
COPY --from=builder /app/poker/game/*.class /app/poker/game/
COPY --from=builder /app/poker/evaluation/*.class /app/poker/evaluation/
COPY --from=builder /app/poker/players/*.class /app/poker/players/
COPY --from=builder /app/poker/ui/*.class /app/poker/ui/
COPY --from=ttyd /usr/bin/ttyd /usr/bin/ttyd

RUN sed -i 's/\r$//' /app/poker/start.sh && chmod +x /app/poker/start.sh

EXPOSE 8080

CMD ["/app/poker/start.sh"]
