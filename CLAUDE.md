# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Development (H2 in-memory, no PostgreSQL needed)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Build (skip tests)
./mvnw clean package -DskipTests

# Run built JAR (dev)
java -Dspring.profiles.active=dev -jar target/ComerciosConecta_Backend-0.0.1-SNAPSHOT.jar

# Docker (backend + PostgreSQL + pgAdmin)
docker-compose up --build
```

Server runs on **port 8080**. Swagger UI at `/swagger-ui.html`. H2 console (dev only) at `/h2-console`.

No test suite is configured.

## Environment Variables

Required for production (`prod` profile). Dev uses H2 and defaults.

```env
SPRING_PROFILES_ACTIVE=dev|prod
JWT_SECRET=<strong-secret>

# PostgreSQL (prod only)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/comerciosdb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<password>

# Wompi payment gateway
WOMPI_PUBLIC_KEY=<key>
WOMPI_PRIVATE_KEY=<key>
WOMPI_EVENTS_SECRET=<secret>

# Factus electronic invoicing
FACTUS_BASE_URL=https://api-sandbox.factus.com.co
FACTUS_USERNAME=<email>
FACTUS_PASSWORD=<password>
FACTUS_CLIENT_ID=<id>
FACTUS_CLIENT_SECRET=<secret>
```

## Architecture

**Java 17 · Spring Boot 3.5.5 · Maven · Spring Data JPA · Spring Security + JWT**

### Package structure

```
com.comerciosconecta.backend/
├── controller/    # REST endpoints
├── service/       # Business logic + external API clients
├── entity/        # JPA entities
├── repository/    # Spring Data JPA interfaces
├── security/      # JWT filter, SecurityConfig, UserDetailsService
├── dto/           # Request/response DTOs
└── config/        # DataInitializer, RestConfig
```

### Database

- **Dev profile:** H2 in-memory, `ddl-auto: update` (schema auto-generated)
- **Prod profile:** PostgreSQL 15 via Docker, `ddl-auto: create` (**⚠️ drops and recreates schema on every restart**), Flyway migrations from `db/migration`

Flyway migrations: `V1` = core schema (comercio, usuario, rol, cliente), `V2` = e-commerce (orders, order_items, payments), `V3` = ventas/invoicing (ventas, venta_items, invoice_records).

`DataInitializer` seeds initial roles and an admin user on startup (dev and prod).

Key entities: `Usuario`, `Producto`, `Venta` / `VentaItem`, `Compra` / `CompraItem`, `Cliente`, `Proveedor`, `Comercio`, `Order` / `OrderItem`, `PaymentRecord`, `InvoiceRecord`.

### Auth

JWT (HS256) via `JwtUtil` + `JwtRequestFilter`. Access token: 15 min. Refresh token: 7 days. Tokens are stateless (no session).

**⚠️ Critical:** The JWT filter is currently **commented out** in `SecurityConfig` — all endpoints are `permitAll()`. Must be re-enabled before production.

CORS allows `http://localhost:3000` and `https://front.vercel.app`.

### API endpoints

| Module | Routes |
|---|---|
| Auth | `POST /api/auth/login` · `/api/auth/refresh` · `/api/auth/logout` |
| Productos | `GET/POST /api/productos` · `GET/PUT/DELETE /api/productos/{id}` |
| Ventas | `GET/POST /api/ventas` · `GET/PUT/DELETE /api/ventas/{id}` |
| Facturación | `POST /api/ventas/{id}/facturar` · `GET /api/ventas/{id}/invoices` |
| Clientes | `GET/POST /api/clientes` · `GET/PUT/DELETE /api/clientes/{id}` |
| Compras | `GET/POST /api/compras` · `GET /api/compras/{id}` |
| Proveedores | `GET/POST /api/proveedores` · `PUT/DELETE /api/proveedores/{id}` |
| Comercios | `GET/POST /api/comercios` |
| E-commerce | `POST /api/checkout/create-order` · `POST /api/checkout/create-payment-link/{orderId}` · `GET /api/checkout/all-orders` |
| Wompi webhook | `POST /api/wompi/webhook` |

### External integrations

**Factus (facturación electrónica colombiana)**

- `FactusClient` service handles OAuth2 token (password grant, cached 50 min) and invoice creation.
- Triggered via `POST /api/ventas/{id}/facturar` → `VentaService.facturarVenta()`.
- **⚠️ Hardcoded establishment data** in `VentaService` ("SuperMarket", address, etc.). Must be parameterized per `Comercio` for multi-tenant use.
- Venta status set to `INVOICED` on success or `ERROR` on failure (frontend displays `ERROR` as "APROBADA" — see frontend code).

**Wompi (pasarela de pagos)**

- `CheckoutService` creates orders and generates Wompi payment links (`checkout.wompi.co`).
- Webhook at `POST /api/wompi/webhook` verifies SHA256 checksum from `WOMPI_EVENTS_SECRET`.
- `APPROVED` → marks order `PAID` and decrements product stock. `DECLINED`/`ERROR` → marks order `FAILED`.

### Frontend connection

The frontend (`ComerciosConecta_Frontend`) connects to this backend via `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api`. The frontend `AuthContext` sends `Authorization: Bearer <token>` on every admin request.
