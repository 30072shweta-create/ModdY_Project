# Environment Setup & Secrets Management Guide

This guide explains how to configure environment variables for local development, staging, and production environments for the Spring Boot Flight Booking Backend.

---

## 1. Quick Start for Local Development

1. Copy `.env.example` to create your local `.env` file:
   ```bash
   cp .env.example .env
   ```
2. Open `.env` and fill in your local credentials (PostgreSQL password, Gmail App Password, JWT secret, etc.).
3. Note: The `.env` file is ignored by Git (`.gitignore`) to ensure secrets are never committed to source control.

---

## 2. Environment Variables Reference

| Variable Name | Purpose | Where to Get Value | Required | Example / Default Format |
|---|---|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | Local or remote PostgreSQL instance host, port, and database name. | Yes | `jdbc:postgresql://localhost:5432/Flight` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL database user name | Configured user in your local PostgreSQL database. | Yes | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL database password | Your local PostgreSQL user's password. | Yes | `your_postgres_password` |
| `SERVER_PORT` | Application HTTP web server port | Unused port on local host machine. | Optional | `8080` |
| `SPRING_MAIL_HOST` | SMTP mail server host | Email provider SMTP settings. | Yes | `smtp.gmail.com` |
| `SPRING_MAIL_PORT` | SMTP mail server port | Email provider TLS/SSL port. | Yes | `587` |
| `SPRING_MAIL_USERNAME` | SMTP account email address | Sender email account. | Yes | `user@example.com` |
| `SPRING_MAIL_PASSWORD` | SMTP account password | Gmail App Password (Security -> 2-Step Verification -> App Passwords). | Yes | `abcd efgh ijkl mnop` |
| `JWT_SECRET` | Secret key for signing & verifying JWT tokens | Random hexadecimal/base64 string (min 256 bits). | Yes | `1e561ab13dc3ac0ba177...` |
| `JWT_ACCESS_EXPIRATION` | JWT Access Token lifetime | Security policy requirement (in ms). | Optional | `900000` (15 mins) |
| `JWT_REFRESH_EXPIRATION` | JWT Refresh Token lifetime | Security policy requirement (in ms). | Optional | `604800000` (7 days) |
| `JWT_COOKIE_NAME` | HttpOnly refresh token cookie name | Contract with frontend client. | Optional | `refreshToken` |
| `JWT_COOKIE_SECURE` | HTTPS requirement for HttpOnly cookie | `false` for local HTTP, `true` for production HTTPS. | Optional | `false` |
| `JWT_COOKIE_SAME_SITE` | SameSite cookie policy | Browser security policy (`Lax`, `Strict`, `None`). | Optional | `Lax` |
| `JWT_COOKIE_PATH` | Cookie path attribute | API path boundary. | Optional | `/` |
| `GOOGLE_CLIENT_ID` | OAuth2 Client ID for Google login | Google Cloud Console Credentials. | Yes | `...apps.googleusercontent.com` |
| `CORS_ALLOWED_ORIGINS` | Permitted frontend origins | Angular frontend client URL. | Yes | `http://localhost:4200` |
| `REDIS_HOST` | Redis cache and seat lock host | Docker container hostname or local IP. | Yes | `localhost` |
| `REDIS_PORT` | Redis server port | Standard Redis port. | Yes | `6379` |
| `REDIS_PASSWORD` | Redis authentication password | Password if Redis auth is enabled; empty otherwise. | Optional | `` |
| `SEAT_LOCK_DURATION_MINUTES` | Temporary seat lock duration | Business rule for holding seat reservations (mins). | Optional | `10` |

---

## 3. GitHub & CI/CD Security Policy

- **Never commit `.env`**: `.env` is listed in `.gitignore` and must never be pushed to GitHub.
- **Tracked Template (`.env.example`)**: Only `.env.example` containing non-sensitive documentation placeholders is tracked in Git.
- **GitHub Secrets**: When deploying via GitHub Actions or cloud providers (AWS, Heroku, Azure, Render), add these variables under **Repository Settings -> Secrets and variables -> Actions**.
