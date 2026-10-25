# BankEase: NextGen Net Banking

> A full-stack net banking application built with **Java Spring Boot**, **Angular 17**, and **MySQL**.

## 🏦 Features

| Module | Features |
|--------|----------|
| **Authentication** | JWT + Refresh tokens, BCrypt passwords, Account lockout, MFA-ready |
| **Account Management** | Multi-account view, balance, statements, date-range filtering |
| **Fund Transfer** | NEFT / RTGS / IMPS with charge calculation, beneficiary management |
| **Bill Pay** | Add billers (electricity, water, mobile, etc.), payment history |
| **Loans** | Apply for loans (Home/Personal/Car/Education), EMI calculator |
| **Investments** | Fixed Deposits & Recurring Deposits with maturity calculator |
| **Security** | RBAC (Customer/Banker/Admin), rate limiting, audit logging |

## 🛠 Tech Stack

- **Backend**: Java 17, Spring Boot 3.2, Spring Security 6, Spring Data JPA
- **Frontend**: Angular 17, Angular Material, Chart.js, RxJS
- **Database**: MySQL 8.0
- **Auth**: JWT (jjwt 0.12.x) + Refresh Token rotation
- **Docs**: SpringDoc OpenAPI / Swagger UI
- **Build**: Maven 3.9, npm, Angular CLI 17

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node 18+
- MySQL 8.0
- Maven 3.9+

### Backend

```bash
cd backend

# Configure database (update application.yml or set env vars)
# DB_USERNAME=root, DB_PASSWORD=yourpassword

mvn clean install
mvn spring-boot:run
```

API runs at: **http://localhost:8080/api**  
Swagger UI: **http://localhost:8080/api/swagger-ui.html**

### Frontend

```bash
cd frontend
npm install
ng serve
```

App runs at: **http://localhost:4200**

### Docker (Full Stack)

```bash
docker-compose up -d
```

## 📂 Project Structure

```
BankEase/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/bankease/api/
│   │   ├── config/             # Security, JPA, OpenAPI config
│   │   ├── controller/         # REST endpoints
│   │   ├── dto/                # Request/Response DTOs
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/             # JPA entities
│   │   ├── exception/          # Custom exceptions & global handler
│   │   ├── repository/         # Spring Data JPA repositories
│   │   ├── security/           # JWT, filters, UserDetails
│   │   ├── service/            # Business logic
│   │   └── util/               # Helpers
│   └── src/main/resources/
│       ├── application.yml     # Configuration
│       └── data.sql            # Seed data
│
├── frontend/                   # Angular application
│   ├── src/app/
│   │   ├── core/               # Guards, interceptors, services
│   │   ├── features/           # Feature modules
│   │   │   ├── auth/
│   │   │   ├── dashboard/
│   │   │   ├── accounts/
│   │   │   ├── transfer/
│   │   │   ├── bills/
│   │   │   ├── loans/
│   │   │   └── investments/
│   │   └── shared/             # Shared components
│   └── src/environments/
│
└── docker-compose.yml
```

## 🔑 Default Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@bankease.com | Admin@12345 |
| Customer (Demo) | rajesh@example.com | Customer@123 |

## 📡 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new customer |
| POST | `/api/v1/auth/login` | Login & get JWT |
| POST | `/api/v1/auth/refresh-token` | Refresh access token |
| GET | `/api/v1/accounts` | Get all accounts |
| GET | `/api/v1/accounts/{no}/statement` | Account statement |
| POST | `/api/v1/transfers` | Fund transfer |
| GET | `/api/v1/beneficiaries` | List beneficiaries |
| POST | `/api/v1/bills/pay` | Pay a bill |
| POST | `/api/v1/loans/apply` | Apply for loan |
| GET | `/api/v1/loans/calculate-emi` | EMI calculator |
| POST | `/api/v1/investments/fd` | Create Fixed Deposit |
| POST | `/api/v1/investments/rd` | Create Recurring Deposit |

## 🔒 Security Features

- **JWT** access tokens (15 min) + **Refresh token** rotation (7 days)
- **BCrypt** password hashing (strength 12)
- **Account lockout** after 5 failed login attempts (30 min)
- **Role-based access control** (CUSTOMER / BANKER / ADMIN)
- **Rate limiting** — 20 requests/min per IP
- **Audit logging** — All sensitive operations logged
- **CORS** — Configurable per environment
- **Input validation** — Jakarta Bean Validation on all DTOs

## 📊 Transfer Charges

| Mode | Charges |
|------|---------|
| IMPS | 0.5% (min ₹5, max ₹15) |
| NEFT | Flat ₹2.50 |
| RTGS | 0.25% (min ₹25) |
