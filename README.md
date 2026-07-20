# BK_SP_Backend

Backend services for the **BK Software Project** — a comprehensive restaurant management system.

---

## Project Overview

The BK Software Project is an enterprise-grade restaurant management platform designed to streamline operations across multiple branches. Built with both **QR* and **online** ordering support, the backend (`BK_SP_Backend`) provides a complete REST API and business logic for managing customers, orders, inventory, staff, branch configuration, and audit logging.

### What It Does

- **🔗 QR-Based Orders**: Generate dynamic QR codes for table sessions; customers scan to access branch-specific menus and place specialty orders directly from their devices. Real-time order tracking and status updates.
- **💻 Online Orders**: Full online ordering system supporting web and mobile clients. Customers can browse menus, customize items, and place orders for delivery or pickup with integrated payment support.
- **Order Management**: Process customer orders (dine-in, takeout, delivery), track fulfillment, and manage order status across all channels.
- **Inventory & Menu Management**: Centralized menu and ingredient inventory tracking across branches with QR code integration.
- **Staff & Access Control**: Role-based access control (RBAC) with staff invitations and privilege management.
- **Branch Configuration**: Support for multi-branch operations with branch-specific settings, QR code configurations, and online ordering preferences.
- **Kitchen Operations**: Real-time kitchen dashboard with order tracking and chef assignments for both QR-based and online orders.
- **Audit & Compliance**: Comprehensive audit logging for all system transactions, staff actions, and order modifications.
- **Customer Portal**: Customer authentication, profile management, order history, QR-code session access, and online order management.
- **Reservation & Table Management**: Smart table booking with capacity checking, automated payment deadlines, receptionist approval workflows, and real-time table occupancy tracking.
- **Admin Dashboard**: Centralized admin panel for business metrics, QR code management, online order analytics, and system management.

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| **Framework** | Spring Boot 3.x |
| **Language** | Java 17+ (Java 24 supported) |
| **Database** | Relational Database (SQL) with JPA/Hibernate ORM |
| **Cache** | Redis (via Spring Data Redis) |
| **Build Tool** | Maven (via Maven Wrapper) |
| **Authentication** | Spring Security with JWT/Session-based auth |
| **Testing** | JUnit 5, Spring Test Framework |
| **Cloud Storage** | AWS S3, Cloudinary |
| **Payments** | Stripe API (Webhooks & Intents) |
| **Utilities** | QR Code generation, Gmail API (OAuth2), TextLK SMS |

---

## Features

### core Features

- **QR-Based Table Ordering**  Generate unique QR codes for each table; customers scan to access branch-specific menus, place orders, request service, and pay directly from their devices. Secure session management with automatic expiration.
- **Comprehensive Online Ordering**: Full-featured online ordering system supporting web and mobile platforms. Real-time menu availability, customizable items, multiple payment methods, and order scheduling (immediate or future delivery/pickup).
- **Multi-tenant Support**: Manage multiple restaurant branches with isolated configurations and QR code namespaces.
- **Dual-Channel Order Processing**: Unified order management across QR-based in-store orders and online orders from web/mobile.
- **RBAC (Role-Based Access Control)**: Granular permissions for Admin, Manager, Chef, Staff, Receptionist, Delivery, and Customer roles.
- **Real-time Order Tracking**: Live updates on order status from placement through kitchen to delivery/pickup for all order channels.
- **Inventory Management**: Dynamic stock tracking with real-time availability sync to QR menus and online platform.
- **Reservation System**: Customer self-service table booking with configurable lead times, guest limits, automated payment windows, and refund processing.
- **Audit Logging**: Track all critical operations, staff actions, order modifications, and system changes for compliance.
- **Multi-language Support**: Foundation for internationalization (i18n) across all order channels.
- **API Rate Limiting**: Protect endpoints from abuse and ensure service reliability.

### Advanced Features

- **Dynamic QR Code Generation**: Create, distribute, and manage QR codes for different tables, promotions, and seasonal menus.
- **Online Menu Customization**: Per-branch menus with time-based availability, seasonal items, and promotional offerings.
- **Kitchen Workflow Optimization**: Assign chefs to orders (QR or online), track preparation time, prioritize orders, and mark completion.
- **Staff Invitation System**: Invite staff members with predefined roles; track acceptance/rejection and shift assignments.
- **AWS S3 Integration**: Securely upload, store, and retrieve dynamic media such as customer profile pictures and menu item reviews via presigned URLs.
- **Payment Integration**: Secure, automated Stripe Checkout integration with fully automated webhook listeners (`payment_intent.succeeded`) to safely verify signatures and update database payment statuses (`PAID`) asynchronously.
- **Delivery Management**: Route optimization and real-time tracking for online delivery orders.
- **Exception Handling**: Robust error handling with detailed audit trails for order cancellations, modifications, and disputes.
- **Branch-Specific Reports**: Sales analytics, inventory insights, and operational reports per branch, segmented by order channel (QR vs. online).
- **Customer Feedback & Ratings**: Collect customer reviews and ratings for orders placed through any channel.

---

## Prerequisites

- **Java 17** or newer (Java 24 is fully supported).
- **Git** (to clone the repository).
- **Maven** is not required locally — the project includes the Maven Wrapper (`mvnw` / `mvnw.cmd`).
- **Database**: MySQL, PostgreSQL, or compatible SQL database (must be provisioned separately).

---

## Project Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd BK_SP_Backend
```

### 2. Environment Configuration

The backend reads configuration from environment variables and property files. Follow the steps below to set up your environment.

#### Create `.env` File

Navigate to `resturarent-system/` and create a `.env` file at the project root:

```bash
cd resturarent-system
touch .env  # On macOS/Linux
# OR
# New-Item -Path .env -ItemType File  # On Windows PowerShell
```

#### `.env` Template

Copy and customize the following template in your `.env` file:

```properties
# Database Configuration
DB_PASSWORD=your_db_password

# JWT/Authentication
JWT_SECRET=your-super-secret-jwt-key-min-32-chars-long
JWT_EXPIRATION_MS=28800000

# SMS Integration (TextLK)
SMS_TEXTLK_TOKEN=your-textlk-token

# Gmail API (OAuth2) Configuration
GMAIL_CLIENT_ID=your-google-oauth-client-id
GMAIL_CLIENT_SECRET=your-google-oauth-client-secret
GMAIL_REFRESH_TOKEN=your-google-oauth-refresh-token
GMAIL_SENDER_EMAIL=your-email@gmail.com
GMAIL_SENDER_NAME=Your App Name

# Frontend URLs & CORS
FRONTEND_ALLOWED_ORIGINS=http://localhost:5173,https://your-domain.com
FRONTEND_STAFF_LOGIN_URL=http://localhost:5173/staff/login
FRONTEND_CUSTOMER_FORGOT_PASSWORD_URL=http://localhost:5173/reset-password
FRONTEND_URL=http://localhost:5173
QR_SCAN_BASE_URL=http://localhost:5173/scan

# AWS S3 Cloud Infrastructure
AWS_S3_BUCKET_NAME=your-bucket-name
AWS_REGION=ap-south-1
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key

# Redis Caching
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Stripe Payment Gateway
STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

Update `src/main/resources/application.properties` to reference environment variables (this is already configured in the repository, just ensure your `.env` keys match).

#### (Alternative) Load `.env` via Maven

If using Maven plugins to load `.env`, add this to `pom.xml`:

```xml
<plugin>
    <groupId>io.github.qwwdfsad</groupId>
    <artifactId>environment-maven-extension</artifactId>
    <version>1.0.0</version>
</plugin>
```

### 3. Database Setup

Create the database and user:

**MySQL:**
```sql
CREATE DATABASE restaurant_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'restaurant_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON restaurant_db.* TO 'restaurant_user'@'localhost';
FLUSH PRIVILEGES;
```

**PostgreSQL:**
```sql
CREATE DATABASE restaurant_db;
CREATE USER restaurant_user WITH ENCRYPTED PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE restaurant_db TO restaurant_user;
```

Update your `.env` file with the credentials.

### 4. Build the Project

```bash
cd resturarent-system
./mvnw clean install
```

**On Windows:**
```powershell
cd resturarent-system
.\mvnw.cmd clean install
```

### 5. Run the Application

#### Development (Quick Start)

```bash
cd resturarent-system
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080/api`

#### Production (Packaged JAR)

```bash
cd resturarent-system
./mvnw -DskipTests package
java -jar target/resturarent-system-*.jar
```

---

## Configuration

### Property Files

- **`src/main/resources/application.properties`** — Base configuration (development defaults).
- **`src/main/resources/application-prod.properties`** — Production-specific overrides.
- **`.env`** — Environment-specific secrets and sensitive data (do not commit).

### Common Configuration Overrides

| Property | Purpose | Example |
|----------|---------|---------|
| `spring.datasource.url` | Database connection string | `jdbc:mysql://localhost:3306/restaurant_db` |
| `spring.datasource.username` | DB username | `restaurant_user` |
| `spring.datasource.password` | DB password | `secure_password` |
| `server.port` | Server port | `8080` |
| `jwt.secret` | JWT signing key | Min 32 characters |
| `spring.profiles.active` | Active Spring profile | `dev`, `prod` |

---

## Testing

### Run All Tests

```bash
cd resturarent-system
./mvnw test
```

### Run Specific Test Class

```bash
./mvnw test -Dtest=YourTestClass
```

### Run with Coverage

```bash
./mvnw clean test jacoco:report
```

---

## Project Structure

```
resturarent-system/
├── src/
│   ├── main/
│   │   ├── java/com/bk/restaurant/
│   │   │   ├── controller/        # REST API endpoints
│   │   │   ├── service/           # Business logic
│   │   │   ├── repository/        # Data access layer
│   │   │   ├── entity/            # JPA entities
│   │   │   ├── dto/               # Data transfer objects
│   │   │   ├── config/            # Spring configuration
│   │   │   ├── security/          # RBAC & authentication
│   │   │   └── util/              # Utility classes
│   │   └── resources/
│   │       ├── application.properties
│   │       └── data.sql           # Initial data
│   └── test/
│       └── java/com/bk/restaurant/
├── pom.xml                         # Maven dependencies
├── mvnw / mvnw.cmd                 # Maven Wrapper
└── .env                            # Environment variables (create manually)
```

---

## Utilities

- **`util/QrCodeGenerator.java`** — Generate QR codes for table sessions and customer interactions.

---

## Contributing

1. Create a feature branch from `dev`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes and test thoroughly.

3. Run the full test suite before committing:
   ```bash
   ./mvnw clean test
   ```

4. Commit with clear messages:
   ```bash
   git commit -m "feat: add feature description"
   ```

5. Push to your branch and create a Pull Request for review.

---

## Troubleshooting

### Port Already in Use

If port 8080 is in use, override in `.env`:
```properties
SERVER_PORT=8081
```

### Database Connection Failed

- Verify database is running and accessible.
- Check `.env` credentials match your database setup.
- Ensure database user has proper permissions.

### Maven Build Fails

Clear the local Maven cache:
```bash
./mvnw clean
./mvnw -U clean install
```

### JPA/Hibernate Issues

Check SQL dialect in `.env` matches your database:
- MySQL: `org.hibernate.dialect.MySQL8Dialect`
- PostgreSQL: `org.hibernate.dialect.PostgreSQLDialect`

---

## Maintainers

For questions or issues, contact the backend team.

---

## License


---

**Last Updated**: July 2026

