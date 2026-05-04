# Backend (Spring Boot)

MVP for "Leiyang Anonymous Task Platform": publish task -> accept -> submit proof -> admin audit -> reward -> withdraw apply (manual review).

## Requirements

- Java 17+
- Maven 3.9+

## Run (dev, default H2)

```bash
cd backend
mvn spring-boot:run
```

Swagger UI:

- `http://localhost:8080/swagger-ui/index.html`

Uploaded files will be saved under `backend/data/uploads/` and served via `/uploads/**`.

## Auth

Mini program (dev only):

- `POST /api/mp/auth/mock-login` with `openId` to get a JWT token.
- Use `Authorization: Bearer <token>` for `/api/mp/**`.

Admin:

- `POST /api/admin/auth/login`
- Default user: `admin`
- Default password: `admin123`

## Production

Use profile `prod` + MySQL and set `APP_JWT_SECRET` (and optionally `APP_CRYPTO_KEY`).

