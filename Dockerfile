FROM eclipse-temurin:21-jdk AS buildstage

RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml .
COPY src /app/src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk

COPY --from=buildstage /app/target/guias-despacho.jar /app/app.jar

EXPOSE 8080

RUN mkdir -p /app/efs

CMD ["java", "-jar", "/app/app.jar"]