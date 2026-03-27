# Estágio de Runtime - Java 21 Alpine (Mais leve e seguro)
FROM eclipse-temurin:21-jre-alpine

# Diretório de trabalho padrão
WORKDIR /app

# Segurança Sênior: Usuário sem privilégios de root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Volume para o Tomcat gerir arquivos temporários
VOLUME /tmp

# Porta padrão desta API (ajuste conforme seu application.yml)
EXPOSE 8096

# O Wildcard (*) garante que o Docker encontre o JAR mesmo que a versão mude no pom.xml
COPY target/*.jar app.jar

# Configurações de JVM:
# urandom melhora a velocidade de geração de tokens JWT no Linux/Docker
ENTRYPOINT ["java", \
            "-Xmx512m", \
            "-Xms256m", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]