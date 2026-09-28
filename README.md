# 🏖️ Titureco — API de Atrações Turísticas

API REST para gerenciamento de atrações turísticas com busca geoespacial, reservas, avaliações e controle de acesso por roles. Desenvolvida com **Spring Boot 3** e **PostGIS**.

[![CI](https://github.com/iago-alves333/api_attractions/actions/workflows/ci.yml/badge.svg)](https://github.com/iago-alves333/api_attractions/actions/workflows/ci.yml)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.2 |
| Banco de dados | PostgreSQL 15 + PostGIS |
| Cache | Redis |
| Autenticação | JWT (Access + Refresh Token com rotação) |
| Migrations | Flyway |
| Mapeamento | MapStruct |
| Rate Limiting | Bucket4j (distribuído via Redis) |
| Observabilidade | Spring Actuator + Prometheus + Grafana |
| Documentação | Swagger / OpenAPI 3 (SpringDoc) |
| CI | GitHub Actions |
| Containerização | Docker + Docker Compose |

---

## Arquitetura

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Frontend   │────▶│   Backend    │────▶│ PostgreSQL   │
│  (Titureco)  │     │ Spring Boot  │     │  + PostGIS   │
└──────────────┘     └──────┬───────┘     └──────────────┘
                           │
                    ┌──────┴───────┐
                    │    Redis     │
                    │ (Cache + RL) │
                    └──────────────┘

┌──────────────┐     ┌──────────────┐
│  Prometheus  │────▶│   Grafana    │
│  (métricas)  │     │ (dashboards) │
└──────────────┘     └──────────────┘
```

### Roles e permissões

| Role | Pode |
|---|---|
| `TOURIST` | Fazer reservas, escrever/editar/excluir reviews, atualizar perfil |
| `GUIDE` | Criar/editar/excluir atrações, confirmar/completar reservas |
| `ADMIN` | Gerenciar usuários, promover tourist → guide, tudo acima |

---

## Endpoints

### Autenticação (`/api/v1/auth`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/register` | Público | Registro de novo usuário |
| `POST` | `/login` | Público | Login (retorna access + refresh token) |
| `POST` | `/refresh` | Público | Rotaciona tokens |
| `POST` | `/logout` | Autenticado | Blacklist do token + revoga refresh tokens |
| `GET` | `/me` | Autenticado | Dados do usuário logado |
| `PUT` | `/me` | Autenticado | Atualizar perfil (nome, email, senha) |

### Atrações (`/api/v1/attractions`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/` | Público | Listar atrações (paginado) |
| `GET` | `/{id}` | Público | Buscar por ID |
| `GET` | `/nearby?lat=&lon=&radiusKm=` | Público | Busca geoespacial por raio |
| `GET` | `/search?keyword=&lat=&lon=&radiusKm=` | Público | Busca textual + geoespacial |
| `POST` | `/` | GUIDE, ADMIN | Criar atração |
| `PUT` | `/{id}` | GUIDE, ADMIN | Editar atração |
| `DELETE` | `/{id}` | GUIDE, ADMIN | Excluir atração |

### Reservas (`/api/v1/reservations`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/` | TOURIST | Criar reserva |
| `GET` | `/` | TOURIST | Listar minhas reservas |
| `GET` | `/{id}` | TOURIST | Ver reserva específica |
| `PATCH` | `/{id}/cancel` | TOURIST | Cancelar reserva |
| `PATCH` | `/{id}/confirm` | GUIDE | Confirmar reserva |
| `PATCH` | `/{id}/complete` | GUIDE | Marcar como concluída |
| `GET` | `/guide` | GUIDE | Reservas das minhas atrações |
| `GET` | `/guide/attractions/{id}` | GUIDE | Reservas de uma atração específica |

### Reviews (`/api/v1/reviews`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/` | Público | Listar reviews |
| `GET` | `/{id}` | Público | Ver review |
| `GET` | `/attraction/{id}` | Público | Reviews de uma atração |
| `POST` | `/` | TOURIST | Criar review |
| `PUT` | `/{id}` | TOURIST | Editar review (autor) |
| `DELETE` | `/{id}` | TOURIST | Excluir review (autor) |

### Usuários (`/api/v1/users`) — ADMIN only

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/` | Criar usuário |
| `GET` | `/` | Listar usuários (paginado) |
| `GET` | `/{id}` | Buscar por ID |
| `DELETE` | `/{id}` | Remover usuário |
| `PATCH` | `/{id}/promote-guide` | Promover tourist → guide |

### Documentação interativa

Swagger UI disponível em: `http://localhost:8080/swagger-ui.html`

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

**Localmente (dev):**
```bash
mvn clean package -DskipTests
java -jar target-maven/*.jar
```

### 4. Acessar os serviços

| Serviço | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| pgAdmin | http://localhost:5050 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

---

## Observabilidade

O Prometheus coleta métricas do Spring Actuator (`/actuator/prometheus`) a cada 15s. O Grafana se conecta ao Prometheus como data source.

Para configurar o Grafana:
1. Acesse `http://localhost:3000` (user: `admin`, senha: definida em `GF_ADMIN_PASSWORD`)
2. Adicione o Prometheus como data source: `http://prometheus:9090`
3. Importe um dashboard (ex: ID `4701` para JVM / Spring Boot)

---

## Testes

```bash
mvn clean test
```

Os testes unitários rodam automaticamente no CI (GitHub Actions) em cada push/PR na branch `main`.

---

## Variáveis de ambiente

| Variável | Descrição | Default |
|---|---|---|
| `DB_HOST` | Host do PostgreSQL | `localhost` |
| `DB_PORT` | Porta do PostgreSQL | `5434` |
| `DB_NAME` | Nome do banco | `titureco` |
| `DB_USER` | Usuário do banco | `postgres` |
| `DB_PASSWORD` | Senha do banco | — |
| `JWT_SECRET` | Chave secreta para assinar JWT (min 32 chars) | — |
| `JWT_EXPIRATION` | Expiração do access token (ms) | `900000` (15min) |
| `JWT_REFRESH_EXPIRATION` | Expiração do refresh token (ms) | `604800000` (7d) |
| `REDIS_HOST` | Host do Redis | `localhost` |
| `REDIS_PORT` | Porta do Redis | `6379` |
| `ADMIN_SEED_PASSWORD` | Senha do admin criado no primeiro boot | — |
| `GF_ADMIN_PASSWORD` | Senha do admin do Grafana | `admin` |
| `RATE_LIMIT_CAPACITY` | Limite de requests por bucket | `100` |
| `RATE_LIMIT_REFILL_TOKENS` | Tokens recarregados por ciclo | `100` |
| `RATE_LIMIT_REFILL_MINUTES` | Intervalo de recarga (min) | `1` |

---

## Estrutura do projeto

```
src/main/java/br/ufpb/iago/backend/
├── config/          # SecurityConfig, CORS, RateLimit, OpenAPI, AdminSeeder
├── controller/      # AuthController, AttractionController, ReservationController, ReviewController, UserController
├── dto/             # Request e Response DTOs
├── exception/       # Exceções de negócio + GlobalExceptionHandler
├── mapper/          # MapStruct mappers
├── model/           # Entidades JPA (User, Attraction, Reservation, Review, RefreshToken, TokenBlacklist)
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter, service, UserDetails, RateLimitFilter
└── service/         # Lógica de negócio
```

---

## Licença

Este projeto foi desenvolvido como trabalho acadêmico na **UFPB** (Universidade Federal da Paraíba).
