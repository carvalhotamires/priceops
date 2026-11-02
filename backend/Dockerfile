# ==========================================
# Estágio 1: Build da aplicação com Maven
# ==========================================
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /build

# Copia arquivos do wrapper do Maven e POM para cache de dependências
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Garante permissão de execução do wrapper e baixa dependências
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copia código-fonte e compila gerando o pacote JAR executável
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Estágio 2: Imagem final de execução (Runtime)
# ==========================================
FROM eclipse-temurin:17-jre-jammy AS runtime

# Instala fontconfig e fontes TrueType (essencial para Apache PDFBox rodar em container headless)
RUN apt-get update && apt-get install -y --no-install-recommends \
    fontconfig \
    fonts-dejavu-core \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Cria usuário não-root por segurança
RUN addgroup --system spring && adduser --system --ingroup spring spring

WORKDIR /app

# Cria diretório padrão para persistência de uploads com permissões corretas
RUN mkdir -p /app/uploads/pdfs && chown -R spring:spring /app

# Copia o JAR compilado do estágio anterior
COPY --from=builder /build/target/*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

# Define usuário de execução
USER spring:spring

EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/produtos || exit 0

# Configurações de JVM otimizadas para contêineres Docker
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]
