# 🏖️ Titureco — API de Atrações Turísticas

API REST para gerenciamento de atrações turísticas com busca geoespacial, reservas, avaliações e controle de acesso por roles. Desenvolvida com **Spring Boot 3** e **PostGIS**.

[![CI](https://github.com/iago-alves333/api_attractions/actions/workflows/ci.yml/badge.svg)](https://github.com/iago-alves333/api_attractions/actions/workflows/ci.yml)

---

## Stack

| Camada | Tecnologia |
|---|---|
| API Gateway | Go (`crypto-gateway` — Reverse Proxy, HMAC-SHA256, Rate Limit) |
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.2 |
| Banco de dados | PostgreSQL 15 + PostGIS |
| Cache | Redis |
| Autenticação | JWT (Access + Refresh Token com rotação) + HMAC-SHA256 (Webhooks no Gateway) |
| Migrations | Flyway |
| Mapeamento | MapStruct |
| Rate Limiting | Redis Lua Script (Gateway) / Bucket4j (fallback standalone) |
| Observabilidade | Spring Actuator + Prometheus + Grafana + `log/slog` |
| Documentação | Swagger / OpenAPI 3 (SpringDoc) |
| CI | GitHub Actions |
| Containerização | Docker + Docker Compose |

---

## Arquitetura

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐     ┌──────────────┐
│   Frontend   │────▶│  Crypto-Gateway  │────▶│   Backend    │────▶│ PostgreSQL   │
│  (Titureco)  │     │   Go (:8080)     │     │ Spring Boot  │     │  + PostGIS   │
└──────────────┘     │  RL + HMAC M2M   │     │ (rede intern)│     └──────────────┘
                     └────────┬─────────┘     └──────┬───────┘
                              │                      │
                              └──────────┬───────────┘
                                         ▼
                                  ┌──────────────┐
                                  │    Redis     │
                                  │ (Cache + RL) │
                                  └──────────────┘

┌──────────────┐     ┌──────────────┐
│  Prometheus  │────▶│   Grafana    │
│ (GW + Boot)  │     │ (dashboards) │
└──────────────┘     └──────────────┘
```

### Roles e permissões

| Role | Pode |
|---|---|
| `TOURIST` | Fazer reservas (com limite diário de vagas), escrever/editar/excluir reviews de passeios concluídos, atualizar perfil |
| `GUIDE` | Criar/editar/excluir suas atrações (sem reservas/reviews vinculadas), confirmar/concluir reservas |
| `ADMIN` | Gerenciar usuários, promover tourist → guide, tudo acima |

---

## Endpoints

### Autenticação (`/api/v1/auth`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/register` | Público | Registro de novo usuário |
| `POST` | `/login` | Público | Login (retorna access + refresh token) |
| `POST` | `/refresh` | Público | Rotaciona tokens atomicamente |
| `POST` | `/logout` | Autenticado | Blacklist do access token + revoga refresh tokens |
| `GET` | `/me` | Autenticado | Dados do usuário logado (`id`, `name`, `email`, `role`, `createdAt`) |
| `PUT` | `/me` | Autenticado | Atualizar perfil (nome, email, senha) |

### Atrações (`/api/v1/attractions`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/?guideId=` | Público | Listar atrações (paginado, filtro opcional por `guideId`) |
| `GET` | `/{id}` | Público | Buscar por ID |
| `GET` | `/nearby?lat=&lon=&radiusKm=` | Público | Busca geoespacial por raio |
| `GET` | `/search?keyword=&lat=&lon=&radiusKm=` | Público | Busca textual + geoespacial |
| `POST` | `/` | GUIDE, ADMIN | Criar atração |
| `PUT` | `/{id}` | GUIDE, ADMIN | Editar atração |
| `DELETE` | `/{id}` | GUIDE, ADMIN | Excluir atração (bloqueado se houver reservas ou reviews vinculadas) |

### Reservas (`/api/v1/reservations`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/` | TOURIST, ADMIN | Criar reserva (lock pessimista por atração + validação de vagas/dia) |
| `GET` | `/` | TOURIST, ADMIN | Listar minhas reservas |
| `GET` | `/{id}` | TOURIST, ADMIN | Ver reserva específica |
| `PATCH` | `/{id}/cancel` | TOURIST, ADMIN | Cancelar reserva |
| `PATCH` | `/{id}/confirm` | GUIDE, ADMIN | Confirmar reserva |
| `PATCH` | `/{id}/complete` | GUIDE, ADMIN | Marcar como concluída |
| `GET` | `/guide` | GUIDE, ADMIN | Reservas das atrações do guia |
| `GET` | `/guide/attractions/{id}` | GUIDE, ADMIN | Reservas de uma atração específica |

### Reviews (`/api/v1/reviews`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/` | Público | Listar reviews |
| `GET` | `/{id}` | Público | Ver review |
| `GET` | `/attraction/{id}` | Público | Reviews de uma atração |
| `POST` | `/` | TOURIST | Criar review (requer reserva `COMPLETED` na atração) |
| `PUT` | `/{id}` | TOURIST | Editar review (autor) |
| `DELETE` | `/{id}` | TOURIST | Excluir review (autor) |

### Usuários (`/api/v1/users`) — ADMIN only

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/` | Criar usuário |
| `GET` | `/` | Listar usuários (paginado) |
| `GET` | `/{id}` | Buscar por ID |
| `DELETE` | `/{id}` | Remover usuário (bloqueado se houver vínculos) |
| `PATCH` | `/{id}/promote-guide` | Promover tourist → guide |

### Webhooks M2M (`/api/v1/webhooks`) — Protegido por HMAC-SHA256 no Gateway

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/reservations/{id}/confirm` | Confirma reserva pendente via callback assinado (`X-Timestamp` + `X-Signature`) |

### Documentação interativa

Swagger UI disponível em `http://localhost:8080/swagger-ui.html` ao rodar o backend diretamente (no Docker Compose com o Gateway na borda, apenas `/api/*` é exposto publicamente).

---

## Setup

### Pré-requisitos

- Docker e Docker Compose
- Java 17+ e Maven (para rodar fora do Docker)

### 1. Clonar e configurar

```bash
git clone https://github.com/iago-alves333/api_attractions.git
cd api_attractions

# Criar o .env a partir do exemplo
cp .env.example .env
```

Edite o `.env` com seus valores:

```env
DB_PASSWORD=uma_senha_forte
JWT_SECRET=gere_com_openssl_rand_hex_32
ADMIN_SEED_PASSWORD=senha_do_admin_inicial
GF_ADMIN_PASSWORD=senha_grafana
```

### 2. Subir a infraestrutura

```bash
# Banco, Redis, Prometheus e Grafana
docker compose up -d db redis prometheus grafana
```

### 3. Rodar o backend

**Com Docker (tudo junto):**
```bash
docker compose up -d
```

**Localmente (dev com dados iniciais de exemplo):**
```bash
mvn clean package -DskipTests
java -jar target-maven/*.jar --spring.profiles.active=dev
```

### 4. Contas iniciais (Seed)

- **AdminSeeder (qualquer profile):** se `ADMIN_SEED_PASSWORD` estiver definida, cria automaticamente o admin `admin@admin.com` no primeiro boot.
- **Profile `dev` (`db/dev/V2__Insert_Initial_Data.sql`):** popula atrações em João Pessoa/PB e contas de teste com a senha `senha123`:
  - **Admin:** `admin@titureco.com`
  - **Guias:** `guia1@titureco.com`, `guia2@titureco.com`
  - **Turistas:** `turista1@titureco.com`, `turista2@titureco.com`

### 5. Acessar os serviços

| Serviço | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| pgAdmin | http://localhost:5050 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

---

## Observabilidade

O Prometheus coleta métricas do **Crypto-Gateway** (`/metrics`: `gateway_http_requests_total`, `gateway_http_request_duration_seconds`) e do **Spring Actuator** (`/actuator/prometheus`) a cada 15s. O Grafana se conecta ao Prometheus como data source.

Para configurar o Grafana:
1. Acesse `http://localhost:3000` (user: `admin`, senha: definida em `GF_ADMIN_PASSWORD`)
2. Adicione o Prometheus como data source: `http://prometheus:9090`
3. Importe um dashboard (ex: ID `4701` para JVM / Spring Boot)

---

## Testes

```bash
mvn clean test
```

A suíte conta com testes automatizados (unitários com JUnit 5/Mockito, testes de slice/segurança e testes de integração reais com **Testcontainers PostGIS + Flyway**), executados no CI (GitHub Actions) a cada push/PR na branch `main`.

---

## Variáveis de ambiente

| Variável | Descrição | Default |
|---|---|---|
| `DB_HOST` | Host do PostgreSQL | `localhost` |
| `DB_PORT` | Porta do PostgreSQL | `5434` |
| `DB_NAME` | Nome do banco | `titureco` |
| `DB_USER` | Usuário do banco | `postgres` |
| `DB_PASSWORD` | Senha do banco | `sua_senha_segura` |
| `JWT_SECRET` | Chave secreta para assinar JWT (min 32 chars) | — |
| `JWT_EXPIRATION` | Expiração do access token (ms) | `900000` (15min) |
| `JWT_REFRESH_EXPIRATION` | Expiração do refresh token (ms) | `604800000` (7d) |
| `REDIS_HOST` | Host do Redis | `localhost` |
| `REDIS_PORT` | Porta do Redis | `6379` |
| `REDIS_PASSWORD` | Senha do Redis (opcional) | vazio |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas no CORS (separadas por vírgula) | `http://localhost:5173,http://localhost:3000` |
| `ADMIN_SEED_PASSWORD` | Senha do `admin@admin.com` criado no primeiro boot | vazio (desativado) |
| `PGADMIN_EMAIL` | E-mail inicial do pgAdmin (Docker) | `admin@titureco.com` |
| `PGADMIN_PASSWORD` | Senha inicial do pgAdmin (Docker) | `admin` |
| `GF_ADMIN_PASSWORD` | Senha do admin do Grafana | `admin` |
| `RATE_LIMIT_ENABLED` | Ativa/desativa `RateLimitFilter` do Spring (`false` atrás do Gateway) | `true` (`false` no Compose) |
| `RATE_LIMIT_CAPACITY` | Limite de requests por bucket (Spring) | `100` |
| `RATE_LIMIT_REFILL_TOKENS` | Tokens recarregados por ciclo (Spring) | `100` |
| `RATE_LIMIT_REFILL_MINUTES` | Intervalo de recarga em min (Spring) | `1` |
| `GATEWAY_CONTEXT` | Caminho do repositório `api_gateway` para build no Compose | `../../GolandProjects/api_gateway` |
| `HMAC_SECRET` | Chave secreta HMAC-SHA256 do Gateway (min 32 chars) | — |
| `HMAC_TOLERANCE` | Janela anti-replay do `X-Timestamp` no Gateway | `5m` |
| `HMAC_PROTECT_ALL` | Exige HMAC em todo `/api/` (além de `/api/v1/webhooks/`) | `false` |
| `GATEWAY_RATE_LIMIT_REQUESTS` | Limite de requisições por IP na janela do Gateway | `100` |
| `GATEWAY_RATE_LIMIT_WINDOW` | Janela de rate limit do Gateway | `1m` |
| `GATEWAY_REDIS_DB` | Banco Redis usado pelo Gateway | `1` |

---

## Estrutura do projeto

```
src/main/java/br/ufpb/iago/backend/
├── config/          # SecurityConfig, CorsConfig, RateLimitConfig, OpenApiConfig, AdminSeeder
├── controller/      # AuthController, AttractionController, ReservationController, ReviewController, UserController, WebhookController
├── dto/             # Request e Response DTOs (Java Records + Bean Validation)
├── exception/       # Exceções de domínio + GlobalExceptionHandler
├── mapper/          # MapStruct mappers
├── model/           # Entidades JPA (User, Attraction, Reservation, Review, RefreshToken)
├── repository/      # Spring Data JPA repositories (queries PostGIS, @EntityGraph, lock pessimista)
├── security/        # JwtAuthenticationFilter, JwtService, CustomUserDetails, RateLimitFilter, HashUtils
└── service/         # Regras de negócio e controle transacional
```

---

## Licença

Este projeto foi desenvolvido como trabalho acadêmico na **UFPB** (Universidade Federal da Paraíba).
