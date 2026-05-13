# Partycentrum Lux Booking Dashboard

Full stack booking management dashboard for Partycentrum Lux.

## Stack

- Backend: Spring Boot, Spring Security JWT, Spring Data JPA, Flyway, JavaMailSender, iText
- Frontend: Angular, PrimeNG, Tailwind CSS, Lucide Icons, Chart.js
- Database: PostgreSQL

## Start Database

```powershell
docker compose up -d postgres
```

If Docker Desktop is not running, start PostgreSQL manually with:

- database: `lux_booking`
- user: `lux`
- password: `lux_password`
- port: `5432`

## Start Backend

```powershell
cd backend
..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

Or, with Maven installed globally:

```powershell
cd backend
mvn spring-boot:run
```

The API runs on `http://localhost:8080/api`.

Seed accounts:

- Owner: `owner@partycentrumlux.nl` / `LuxAdmin123!`
- Employee: `employee@partycentrumlux.nl` / `LuxEmployee123!`

## Start Frontend

```powershell
cd frontend
npm.cmd install
npm.cmd start
```

The dashboard runs on `http://localhost:4200`.

## Mail Configuration

Gmail SMTP is disabled by default. Enable it with environment variables:

```powershell
$env:MAIL_ENABLED="true"
$env:MAIL_USERNAME="your-gmail-address@gmail.com"
$env:MAIL_PASSWORD="your-gmail-app-password"
$env:MAIL_FROM="your-gmail-address@gmail.com"
$env:OWNER_EMAIL="owner@partycentrumlux.nl"
```

Use a Gmail app password, not a normal Gmail password.

## Useful Backend Environment Variables

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `INVOICE_STORAGE_DIR`
- `COMPANY_LOGO_PATH`

## Included API Areas

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET/POST/PUT/DELETE /api/users` owner only
- `GET/POST/PUT/DELETE /api/customers`
- `GET/POST/PUT/DELETE /api/bookings`
- `GET /api/bookings/calendar`
- `GET/POST/PUT/DELETE /api/invoices` owner only
- `POST /api/invoices/{id}/generate-pdf`
- `GET /api/invoices/{id}/download`
- `POST /api/invoices/{id}/mark-paid`
- `POST /api/invoices/{id}/send-reminder`
- `GET/POST/PUT/DELETE /api/payments` owner only
- `GET /api/dashboard/stats` owner only
- `GET/PUT /api/settings` owner only
- `POST /api/settings/logo` owner only

## Automations

- Booking confirmation email when a booking becomes `BEVESTIGD`
- Daily overdue invoice reminders
- Thank-you email one day after a confirmed event
- Weekly owner summary every Monday morning

Scheduled jobs use the `Europe/Amsterdam` timezone.
