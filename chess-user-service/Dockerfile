FROM bellsoft/liberica-openjdk-alpine:21-cds AS builder
RUN apk add --no-cache ca-certificates && update-ca-certificates
WORKDIR /application
COPY . .
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw && ./mvnw clean install -DskipTests -pl chess-user-service -am

FROM bellsoft/liberica-openjre-alpine:21-cds AS layers
WORKDIR /application
COPY --from=builder /application/chess-user-service/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM bellsoft/liberica-openjre-alpine:21-cds

VOLUME /tmp

RUN apk add --no-cache ca-certificates curl && update-ca-certificates
RUN adduser -S spring-user
USER spring-user

WORKDIR /application

COPY --from=layers /application/app.jar ./
COPY --from=layers /application/extracted/dependencies/ ./
COPY --from=layers /application/extracted/spring-boot-loader/ ./
COPY --from=layers /application/extracted/snapshot-dependencies/ ./
COPY --from=layers /application/extracted/application/ ./

RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar app.jar || true

ENV JAVA_CDS_OPTS="-XX:SharedArchiveFile=app.jsa -Xlog:class+load:file=/tmp/classload.log"
ENV JAVA_ERROR_FILE_OPTS="-XX:ErrorFile=/tmp/java_error.log"
ENV JAVA_HEAP_DUMP_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"
ENV JAVA_ON_OUT_OF_MEMORY_OPTS="-XX:+ExitOnOutOfMemoryError"
EXPOSE 8082

ENTRYPOINT exec java \
    $JAVA_HEAP_DUMP_OPTS \
    $JAVA_ON_OUT_OF_MEMORY_OPTS \
    $JAVA_ERROR_FILE_OPTS \
    $JAVA_CDS_OPTS \
    -jar app.jar
