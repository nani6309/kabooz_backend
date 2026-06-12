# Kabooz Goli Soda — Backend API
### Sri Rama Krupa Enterprises | Spring Boot 3.2.x

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.x running locally

### 1. Database Setup
```sql
-- Run schema.sql in your MySQL client:
mysql -u root -p < src/main/resources/schema.sql
```

### 2. Configure application.properties
```properties
spring.datasource.password=YOUR_ACTUAL_MYSQL_PASSWORD
```

### 3. Build & Run
```bash
mvn clean install -DskipTests
mvn spring-boot:run
```
Server starts on **http://localhost:8080**

---

## Default Admin Credentials
| Field    | Value         |
|----------|---------------|
| Username | `admin`       |
| Password | `kabooz@2024` |

---

## API Endpoints

### Authentication
| Method | Path             | Auth | Description         |
|--------|-----------------|------|---------------------|
| POST   | `/api/auth/login` | None | Admin login → JWT  |

**Login Body:**
```json
{
  "username": "admin",
  "password": "kabooz@2024"
}
```
**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 86400,
  "username": "admin",
  "idleTimeoutSeconds": 600
}
```

---

### Public Endpoints (No Auth)
| Method | Path                  | Description                     |
|--------|-----------------------|---------------------------------|
| POST   | `/api/public/orders`  | Place order from homepage       |
| GET    | `/api/public/pricing` | Get full pricing table          |

---

### Admin Endpoints (Bearer JWT Required)
| Method | Path                              | Description                   |
|--------|-----------------------------------|-------------------------------|
| GET    | `/api/admin/dashboard`            | Dashboard stats               |
| GET    | `/api/admin/orders`               | Paginated order list          |
| GET    | `/api/admin/orders/{id}`          | Full order details            |
| POST   | `/api/admin/orders`               | Create order (admin)          |
| PUT    | `/api/admin/orders/{id}/status`   | Update status + payment       |
| DELETE | `/api/admin/orders/{id}`          | Soft delete order             |
| GET    | `/api/admin/orders/{id}/invoice/pdf` | Download PDF invoice       |

**Query params for GET /api/admin/orders:**
- `page` (default: 0)
- `size` (default: 20)
- `status` (all | PENDING | PAID | OVERDUE)
- `search` (customer name / mobile / invoice no)

---

## Security

### JWT Authentication
- All `/api/admin/**` endpoints require `Authorization: Bearer <token>`
- Tokens expire after **24 hours**
- **10-minute idle timeout** enforced on the frontend using `idleTimeoutSeconds` hint

### Brute-Force Protection
- After **5** consecutive failed login attempts, the account is locked for **15 minutes**
- Lock status is persisted in MySQL (`admin_users.locked_until`)

---

## Pricing Rules

### Glass Bottles (1 crate = 24 bottles)
| ₹/bottle | Crate Total |
|----------|-------------|
| ₹20      | ₹480        |
| ₹21      | ₹504        |
| ₹22      | ₹528        |
| ₹23      | ₹552        |
| ₹24      | ₹576        |
| ₹25      | ₹600        |

### PET Bottles (1 case = 30 bottles)
| ₹/bottle | Case Total |
|----------|------------|
| ₹22      | ₹660       |
| ₹23      | ₹690       |
| ₹24      | ₹720       |
| ₹25      | ₹750       |

**Tax:** All prices are **tax-inclusive** (CGST 20% + SGST 20% = 40% total)
- `Taxable = Price / 1.4`
- `CGST = Taxable × 20%`
- `SGST = Taxable × 20%`

---

## Error Response Format
All errors return a consistent JSON structure:
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "mobile must be exactly 10 digits",
  "timestamp": "2026-06-10T10:30:00"
}
```

---

## CORS Origins Allowed
- `http://localhost:5173` (Vite dev)
- `http://localhost:3000` (React dev)
- `https://kabooz.in`
- `https://www.kabooz.in`
- `https://admin.kabooz.in`

---

## Project Structure
```
src/main/java/com/kabooz/backend/
├── config/           SecurityConfig, CorsConfig, AppConfig
├── controller/       AuthController, PublicOrderController, AdminController
├── service/          AuthService, OrderService, InvoiceService, PricingService
├── repository/       AdminUserRepository, CustomerRepository, OrderRepository, InvoiceCounterRepository
├── entity/           AdminUser, Customer, Order, OrderItem, InvoiceCounter
├── dto/
│   ├── request/      LoginRequest, PlaceOrderRequest, AdminOrderRequest, UpdateOrderStatusRequest
│   └── response/     AuthResponse, OrderResponse, OrderSummaryResponse, DashboardStatsResponse, PricingResponse, PlaceOrderResponse
├── security/         JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl
├── exception/        GlobalExceptionHandler, OrderNotFoundException, UnauthorizedException, InvalidPricingException
└── KaboozApplication.java
```

---

## Frontend Integration

### Admin Idle Logout (10 minutes)
The backend returns `idleTimeoutSeconds: 600` in the login response.
Implement in React Admin:
```javascript
let idleTimer;
const resetTimer = () => {
  clearTimeout(idleTimer);
  idleTimer = setTimeout(() => {
    localStorage.removeItem('token');
    window.location.href = '/admin/login';
  }, 600_000); // 10 minutes
};
['mousemove', 'keydown', 'click', 'scroll'].forEach(e =>
  document.addEventListener(e, resetTimer)
);
resetTimer();
```

---

## Company Details (Invoice)
- **Company:** SRI RAMA KRUPA ENTERPRISES
- **GSTIN:** 29JAMPK0701B1ZY
- **Phone:** 8123980893
- **Email:** kaboozgolisoda1528@gmail.com
- **Address:** Ground No.46/4, Muthanallur Cross, Off-Sarjapura Road, Dommasandra, Bengaluru 562125
- **Bank:** Canara Bank DOMMASANDRA | AC: 120033317947 | IFSC: CNRB0004789
- **UPI:** kushalreddy1680-1@okaxis
# kabooz_backend
