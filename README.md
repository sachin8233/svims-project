# Smart Vendor & Invoice Management System (SVIMS)

A comprehensive full-stack enterprise application for managing vendors, invoices, payments, and approval workflows with GST calculation, risk scoring, automated reminders, and an **AI-powered chatbot** for intelligent assistance.

## 📖 Project Overview

SVIMS (Smart Vendor & Invoice Management System) is a production-ready enterprise application designed to streamline vendor management, invoice processing, payment tracking, and multi-level approval workflows. The system features:

- **Multi-level Approval Workflow**: Dynamic approval rules based on invoice amounts
- **Automated GST Calculation**: CGST/SGST/IGST calculation based on vendor and invoice locations
- **Vendor Risk Scoring**: Intelligent risk assessment algorithm (0-100 scale)
- **AI-Powered Chatbot**: Role-aware intelligent assistant with GenAI integration (OpenAI/Groq)
- **Complete Audit Trail**: Comprehensive logging of all user actions
- **Scheduled Automation**: Automated reminders, overdue marking, and escalation
- **Role-Based Access Control**: Secure access management with JWT authentication

## 🚀 Technology Stack

### Backend
- **Spring Boot 3.2.0** - Framework
- **Spring Security** - Authentication & Authorization
- **JWT** - Token-based authentication
- **Hibernate/JPA** - ORM
- **PostgreSQL** - Database
- **Swagger/OpenAPI** - API Documentation
- **Maven** - Build tool

### Frontend
- **Angular 15+** - Framework
- **TypeScript** - Language
- **RxJS** - Reactive programming
- **Bootstrap 5** - UI framework
- **Chart.js** - Data visualization

## 📋 Features

### Backend Features
- ✅ Vendor CRUD operations with GSTIN validation
- ✅ Invoice CRUD with automatic GST calculation (CGST/SGST/IGST)
- ✅ Payment tracking (partial & full payments)
- ✅ Multi-level approval workflow based on invoice amount (configurable rules)
- ✅ GST calculation based on vendor and invoice location
- ✅ Scheduled tasks for:
  - Due-date reminders
  - Auto-escalation of overdue invoices
  - Marking overdue invoices
- ✅ Complete audit trail (who did what, when, old vs new values)
- ✅ Vendor risk scoring based on:
  - Late payments
  - Overdue invoices
  - Payment ratio
  - Escalated invoices
- ✅ **AI-Powered Chatbot** with:
  - GenAI integration (OpenAI GPT-3.5/GPT-4 or Groq)
  - RAG (Retrieval Augmented Generation) pattern
  - Role-aware responses based on user permissions
  - Natural language understanding
  - Fallback to rule-based pattern matching
- ✅ Spring Security with JWT authentication
- ✅ Role-based access control (Admin, Manager, Finance, User)
- ✅ RESTful APIs with Swagger documentation

### Frontend Features
- ✅ Role-based dashboards with real-time statistics
- ✅ Reactive forms with validation
- ✅ Lazy-loaded modules for better performance
- ✅ JWT authentication with interceptors
- ✅ Route guards for authentication and authorization
- ✅ Responsive UI with Bootstrap 5
- ✅ **AI Chatbot Interface** with:
  - Modern chat UI with typing indicators
  - Quick question suggestions
  - Real-time message display
  - Role-based query suggestions
- ✅ Invoice list with search, pagination, and filtering
- ✅ Payment reports and analytics
- ✅ Approval history tracking

## 📁 Project Structure

```
PracticeProject/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/svims/
│   │   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── repository/     # Data access layer
│   │   │   │   ├── service/        # Business logic
│   │   │   │   ├── controller/     # REST controllers
│   │   │   │   ├── dto/            # Data transfer objects
│   │   │   │   ├── config/         # Configuration classes
│   │   │   │   ├── security/       # Security configuration
│   │   │   │   └── scheduler/      # Scheduled tasks
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── db/              # SQL scripts
│   │   └── test/
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── auth/               # Authentication
│   │   │   ├── dashboard/         # Dashboard
│   │   │   ├── vendors/           # Vendor module (lazy-loaded)
│   │   │   ├── invoices/          # Invoice module (lazy-loaded)
│   │   │   ├── payments/          # Payment module (lazy-loaded)
│   │   │   ├── services/          # Angular services
│   │   │   ├── guards/            # Route guards
│   │   │   └── interceptors/      # HTTP interceptors
│   │   ├── index.html
│   │   └── styles.css
│   ├── package.json
│   └── angular.json
└── README.md
```

## 🛠️ Setup Instructions

### Prerequisites
- Java 21 or higher
- Maven 3.6+
- Node.js 18+ and npm
- PostgreSQL 12+
- Angular CLI 15+
- (Optional) OpenAI API key or Groq API key for AI chatbot

### Backend Setup

1. **Clone the repository**
   ```bash
   cd backend
   ```

2. **Configure Database**
   - Create PostgreSQL database:
     ```sql
     CREATE DATABASE svims_db;
     ```
   - Update `src/main/resources/application.properties`:
     ```properties
     spring.datasource.username=postgres
     spring.datasource.password=your_password
     ```

3. **Run Database Scripts** (Optional - Hibernate will create tables automatically)
   ```bash
   psql -U postgres -d svims_db -f src/main/resources/db/schema.sql
   psql -U postgres -d svims_db -f src/main/resources/db/seed-data.sql
   ```

4. **Configure AI Chatbot (Optional)**
   - For OpenAI: Get API key from https://platform.openai.com/api-keys
   - For Groq (Free): Get API key from https://console.groq.com
   - Update `src/main/resources/application.properties`:
     ```properties
     # Option 1: OpenAI
     openai.enabled=true
     openai.api.key=${OPENAI_API_KEY:}
     openai.model=gpt-3.5-turbo
     
     # Option 2: Groq (Free tier)
     chatbot.enabled=true
     chatbot.provider=groq
     groq.api.key=${GROQ_API_KEY:}
     groq.model=llama-3.1-8b-instant
     
     # Option 3: Simple rule-based (No API key needed)
     chatbot.enabled=true
     chatbot.provider=simple
     ```
   - Or set environment variables:
     ```bash
     export OPENAI_API_KEY=sk-your-key-here
     # OR
     export GROQ_API_KEY=gsk-your-key-here
     ```

5. **Build and Run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

6. **Access Swagger UI**
   - Open browser: `http://localhost:8080/swagger-ui/index.html`

### Frontend Setup

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Run development server**
   ```bash
   ng serve
   ```

4. **Access Application**
   - Open browser: `http://localhost:4200`

### Default Credentials

- **Admin**: `admin` / `admin123`
- **Manager**: `manager1` / `password123`
- **Finance**: `finance1` / `password123`

## 📊 Database Schema

### Key Tables
- **users** - System users with roles
- **roles** - User roles (ADMIN, MANAGER, FINANCE)
- **vendors** - Vendor information
- **invoices** - Invoice details with GST
- **payments** - Payment records
- **approval_rules** - Dynamic approval rules
- **invoice_approvals** - Approval history
- **audit_logs** - Complete audit trail

### Relationships
- Vendor → Invoices (One-to-Many)
- Invoice → Payments (One-to-Many)
- Invoice → InvoiceApprovals (One-to-Many)
- User → Roles (Many-to-Many)

## 🔐 Security

- JWT-based authentication
- Password encryption with BCrypt
- Role-based access control:
  - **ADMIN**: Full access
  - **MANAGER**: Vendor & Invoice management
  - **FINANCE**: Payment operations
- CORS configuration for frontend
- HTTP interceptors for token management

## 📈 API Endpoints

### Authentication
- `POST /api/auth/login` - User login

### Vendors
- `GET /api/vendors` - Get all vendors
- `GET /api/vendors/{id}` - Get vendor by ID
- `POST /api/vendors` - Create vendor
- `PUT /api/vendors/{id}` - Update vendor
- `DELETE /api/vendors/{id}` - Delete vendor
- `GET /api/vendors/high-risk` - Get high-risk vendors

### Invoices
- `GET /api/invoices` - Get all invoices
- `GET /api/invoices/{id}` - Get invoice by ID
- `POST /api/invoices` - Create invoice
- `PUT /api/invoices/{id}` - Update invoice
- `DELETE /api/invoices/{id}` - Delete invoice
- `POST /api/invoices/{id}/approve` - Approve invoice
- `POST /api/invoices/{id}/reject` - Reject invoice
- `GET /api/invoices/overdue` - Get overdue invoices

### Payments
- `GET /api/payments` - Get all payments
- `GET /api/payments/{id}` - Get payment by ID
- `GET /api/payments/invoice/{invoiceId}` - Get payments by invoice
- `POST /api/payments` - Create payment
- `DELETE /api/payments/{id}` - Delete payment

### AI Chatbot
- `POST /api/chatbot/chat` - Send message to AI chatbot
- `GET /api/chatbot/help` - Get help information based on user role

### Reports & Analytics
- `GET /api/reports/summary` - Get system summary statistics
- `GET /api/reports/payments` - Get payment reports
- `GET /api/reports/approvals` - Get approval statistics

## 🎯 Business Logic Highlights

### GST Calculation
- **Same State**: CGST (9%) + SGST (9%) = 18%
- **Different State**: IGST (18%)
- Automatically calculated based on vendor GSTIN

### Approval Workflow
- Dynamic rules based on invoice amount ranges
- Multi-level approvals (1-4 levels)
- Configurable required roles per level
- Tracks approval history

### Risk Scoring Algorithm
- **Overdue Invoices**: 40 points max
- **Late Payments**: 30 points max
- **Payment Ratio**: 20 points max
- **Escalated Invoices**: 10 points max
- **Total**: 0-100 scale (higher = riskier)

### Scheduled Tasks
- **Daily 8 AM**: Send due-date reminders (3 days before)
- **Daily 9 AM**: Mark overdue invoices
- **Daily 10 AM**: Escalate overdue invoices

### AI Chatbot Features
- **RAG Pattern**: Retrieves actual data from database, augments with role context, generates natural responses
- **Role-Aware**: Different responses based on user role (ADMIN, MANAGER, FINANCE, USER)
- **GenAI Integration**: Supports OpenAI GPT-3.5/GPT-4 or Groq (free tier available)
- **Intelligent Queries**: Understands natural language questions about:
  - Invoice status and details
  - Pending approvals
  - Payment information
  - System statistics
  - Vendor information
  - Approval history
- **Fallback Support**: Rule-based pattern matching if GenAI is disabled
- **Secure**: All queries respect role-based access control

## 📝 Resume Bullets

### For Backend Developer
- Developed a **Spring Boot microservice** for vendor and invoice management with **RESTful APIs** handling 10+ endpoints
- Implemented **JWT-based authentication** with Spring Security and role-based access control (Admin, Manager, Finance)
- Designed and implemented **multi-level approval workflow** with dynamic rules stored in database, supporting 1-4 approval levels based on invoice amount
- Created **automated GST calculation engine** (CGST/SGST/IGST) based on vendor and invoice location with 18% tax rate
- Built **vendor risk scoring algorithm** (0-100 scale) considering late payments, overdue invoices, payment ratio, and escalations
- Implemented **scheduled tasks** using Spring Scheduler for due-date reminders, overdue marking, and auto-escalation
- Designed **complete audit trail system** logging all user actions with old/new values, timestamps, and IP addresses
- Optimized database queries with **Hibernate indexing** on frequently queried fields (invoice_date, status, vendor_id)
- Integrated **Swagger/OpenAPI** for comprehensive API documentation
- Followed **layered architecture** (Controller → Service → Repository) with proper transaction management

### For Full-Stack Developer
- Built **end-to-end application** using Spring Boot backend and Angular 15+ frontend with JWT authentication
- Implemented **lazy-loaded modules** in Angular for better performance and code splitting
- Created **reactive forms** with validation for vendor, invoice, and payment management
- Developed **role-based dashboards** with real-time statistics and charts
- Implemented **HTTP interceptors** for automatic token attachment and error handling
- Designed **responsive UI** using Bootstrap 5 with role-based navigation and access control
- Created **RESTful APIs** with proper error handling, validation, and Swagger documentation

## 💼 Interview Q&A

### Q1: Explain the architecture of your SVIMS project.
**A:** SVIMS follows a **layered architecture**:
- **Controller Layer**: REST endpoints with Swagger documentation, handles HTTP requests/responses
- **Service Layer**: Business logic (GST calculation, approval workflow, risk scoring, audit logging)
- **Repository Layer**: Data access using Spring Data JPA with custom queries
- **Entity Layer**: JPA entities with proper relationships and indexing
- **Security Layer**: JWT authentication filter and role-based authorization
- **Frontend**: Angular with lazy-loaded modules, services, guards, and interceptors

### Q2: How does the multi-level approval workflow work?
**A:** 
1. **Dynamic Rules**: Approval rules are stored in `approval_rules` table with amount ranges and required levels
2. **Rule Matching**: When invoice is created, system finds applicable rule based on invoice amount
3. **Level Tracking**: Each invoice tracks `current_approval_level` (0 to max levels)
4. **Approval Process**: Users approve at specific levels, creating `InvoiceApproval` records
5. **Status Update**: When all required levels are approved, invoice status changes to APPROVED
6. **Role-based**: Each level can require specific roles (MANAGER, FINANCE, ADMIN)

### Q3: Explain the GST calculation logic.
**A:**
- **Input**: Base amount, vendor state (from GSTIN), invoice state
- **Same State Logic**: If vendor and invoice are in same state → CGST (9%) + SGST (9%) = 18%
- **Different State Logic**: If different states → IGST (18%)
- **State Extraction**: First 2 digits of GSTIN represent state code
- **Calculation**: `GST = (Amount × Rate) / 100`
- **Total**: `Total = Amount + CGST + SGST + IGST`

### Q4: How does vendor risk scoring work?
**A:** Risk score (0-100) calculated from:
1. **Overdue Invoices** (40 pts): Count × 10, max 40
2. **Late Payments** (30 pts): Payments after due date, count × 5, max 30
3. **Payment Ratio** (20 pts): `(1 - paid_amount/total_amount) × 20`
4. **Escalated Invoices** (10 pts): Count × 5, max 10
- Higher score = higher risk
- Updated when payments are made or invoices become overdue

### Q5: How do you handle security in this application?
**A:**
1. **JWT Authentication**: Token-based stateless authentication
2. **Password Encryption**: BCrypt hashing (strength 10)
3. **Role-Based Access**: `@PreAuthorize` annotations on controllers
4. **CORS Configuration**: Allowed origins for frontend
5. **HTTP Interceptors**: Automatic token attachment in Angular
6. **Route Guards**: Protect routes based on authentication and roles
7. **Security Filter Chain**: JWT filter validates tokens before requests reach controllers

### Q6: Explain the scheduled tasks implementation.
**A:**
- **@EnableScheduling**: Enabled in main application class
- **@Scheduled**: Cron expressions for timing
  - `0 0 8 * * ?` - 8 AM daily (reminders)
  - `0 0 9 * * ?` - 9 AM daily (mark overdue)
  - `0 0 10 * * ?` - 10 AM daily (escalate)
- **Tasks**:
  1. Find invoices due in next 3 days → send reminders
  2. Find invoices past due date → mark as overdue
  3. Find overdue invoices → increment escalation level

### Q7: How is the audit trail implemented?
**A:**
1. **AuditService**: Separate service with `@Transactional(REQUIRES_NEW)` to ensure logging even if main transaction fails
2. **Logging Points**: All CRUD operations, approvals, rejections
3. **Data Captured**:
   - User name, action type, entity type/ID
   - Old value (JSON), new value (JSON)
   - IP address, timestamp, description
4. **ObjectMapper**: Converts objects to JSON for storage
5. **Async Logging**: Uses separate transaction to avoid blocking main operations

### Q8: What optimizations did you implement?
**A:**
1. **Database Indexing**: Indexes on `invoice_date`, `status`, `vendor_id`, `due_date`
2. **Lazy Loading**: Hibernate lazy loading for relationships
3. **Lazy Modules**: Angular lazy-loaded modules for code splitting
4. **Query Optimization**: Custom JPQL queries with proper joins
5. **Transaction Management**: `@Transactional` for data consistency
6. **Caching Ready**: Structure supports Spring Cache integration

### Q9: How do you handle partial payments?
**A:**
1. **Validation**: Check if payment amount ≤ remaining invoice amount
2. **Status Update**: 
   - If `total_paid >= invoice_total` → Status = PAID
   - If `total_paid < invoice_total` → Status = PARTIALLY_PAID
3. **Calculation**: `remaining = invoice_total - sum(all_payments)`
4. **Payment Records**: Each payment stored separately with invoice reference

### Q10: Explain the frontend architecture.
**A:**
- **Modules**: Feature modules (vendors, invoices, payments) with lazy loading
- **Services**: HTTP services for API communication
- **Guards**: AuthGuard (authentication), RoleGuard (authorization)
- **Interceptors**: AuthInterceptor (adds JWT token), ErrorInterceptor (handles 401)
- **Components**: 
  - List components (display data)
  - Form components (create/edit with reactive forms)
  - Dashboard (statistics and charts)
- **Routing**: Route-based code splitting with guards

### Q11: How does the AI Chatbot work?
**A:**
1. **RAG Pattern**: 
   - **Retrieval**: Fetches actual data from database based on query
   - **Augmentation**: Adds role context and permissions to the data
   - **Generation**: Uses GenAI (OpenAI/Groq) to generate natural language response
2. **Role-Based Filtering**: 
   - USER: Can only see PENDING/APPROVED invoices
   - MANAGER: Can see pending approvals and statistics
   - FINANCE: Can see payment information
   - ADMIN: Full access to all data
3. **Hybrid Approach**: 
   - Primary: GenAI with RAG (if enabled)
   - Fallback: Rule-based pattern matching (if GenAI disabled/fails)
4. **Security**: All queries respect JWT authentication and role-based access control
5. **Natural Language**: Understands questions like "What invoices need my attention?" and provides contextual responses

## 🔧 Troubleshooting

### Backend Issues
- **Port 8080 already in use**: Change `server.port` in `application.properties`
- **Database connection error**: Verify PostgreSQL credentials and database exists
- **JWT token expired**: Default expiration is 24 hours, can be adjusted

### Frontend Issues
- **CORS error**: Ensure backend CORS configuration allows `http://localhost:4200`
- **401 Unauthorized**: Check if JWT token is being sent in Authorization header
- **Module not found**: Run `npm install` to install dependencies

## 🤖 AI Chatbot Setup

### Quick Start (Rule-Based - No API Key Needed)
The chatbot works immediately with rule-based pattern matching. No configuration needed!

### Enable GenAI (OpenAI)
1. Get API key from [OpenAI Platform](https://platform.openai.com/api-keys)
2. Set environment variable:
   ```bash
   export OPENAI_API_KEY=sk-your-key-here
   ```
3. Update `application.properties`:
   ```properties
   openai.enabled=true
   openai.api.key=${OPENAI_API_KEY:}
   openai.model=gpt-3.5-turbo
   ```

### Enable GenAI (Groq - Free Tier)
1. Get API key from [Groq Console](https://console.groq.com) (Free tier available)
2. Set environment variable:
   ```bash
   export GROQ_API_KEY=gsk-your-key-here
   ```
3. Update `application.properties`:
   ```properties
   chatbot.enabled=true
   chatbot.provider=groq
   groq.api.key=${GROQ_API_KEY:}
   groq.model=llama-3.1-8b-instant
   ```

### Chatbot Capabilities by Role

**USER Role:**
- "What is the status of my invoice?"
- "Which manager is handling my invoice?"
- "How do I create an invoice?"

**MANAGER Role:**
- "Which invoices are pending for approval?"
- "Show approval statistics"
- "What are the pending approvals?"
- "Suggest a rejection reason for this invoice"

**FINANCE Role:**
- "Which invoices are ready for payment?"
- "Show payment statistics"
- "How many payments were processed?"
- "What's the total amount paid?"

**ADMIN Role:**
- "Show a summary of total invoices and payments"
- "What are the system statistics?"
- "How many users are in the system?"
- All queries from other roles

For detailed chatbot documentation, see [CHATBOT_DOCUMENTATION.md](CHATBOT_DOCUMENTATION.md) and [GENAI_SETUP.md](GENAI_SETUP.md)

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Angular Documentation](https://angular.io/docs)
- [JWT.io](https://jwt.io/) - JWT token decoder
- [Swagger UI](http://localhost:8080/swagger-ui/index.html) - API documentation
- [OpenAI API Documentation](https://platform.openai.com/docs)
- [Groq API Documentation](https://console.groq.com/docs)

## 📄 License

This project is for educational and portfolio purposes.

## 🎯 Key Project Highlights

### Architecture
- **Layered Architecture**: Controller → Service → Repository pattern
- **Separation of Concerns**: Clear boundaries between layers
- **RESTful Design**: Standard HTTP methods and status codes
- **Microservices Ready**: Can be split into separate services

### Security
- **JWT Authentication**: Stateless token-based authentication
- **BCrypt Password Hashing**: Secure password storage
- **Role-Based Access Control**: Fine-grained permissions
- **CORS Configuration**: Secure cross-origin requests
- **SQL Injection Protection**: Parameterized queries via JPA

### Performance
- **Database Indexing**: Optimized queries on frequently accessed fields
- **Lazy Loading**: Efficient data fetching with Hibernate
- **Lazy Module Loading**: Code splitting in Angular
- **Pagination**: Efficient data retrieval for large datasets

### AI/ML Integration
- **GenAI Integration**: OpenAI GPT-3.5/GPT-4 or Groq support
- **RAG Pattern**: Retrieval Augmented Generation for contextual responses
- **Role-Aware AI**: Intelligent responses based on user permissions
- **Fallback Mechanism**: Rule-based pattern matching when AI unavailable

### Business Logic
- **Dynamic Approval Rules**: Configurable multi-level approval workflow
- **Automated GST Calculation**: CGST/SGST/IGST based on location
- **Risk Scoring Algorithm**: Intelligent vendor risk assessment
- **Scheduled Automation**: Automated reminders and escalations
- **Complete Audit Trail**: Comprehensive activity logging

## 📊 Project Statistics

- **Backend**: 65+ Java files
- **Frontend**: 30+ Angular components
- **API Endpoints**: 40+ REST endpoints
- **Database Tables**: 10+ tables with relationships
- **Roles Supported**: 4 roles (ADMIN, MANAGER, FINANCE, USER)
- **Technologies**: 15+ technologies integrated

## 🚀 Future Enhancements

- [ ] Real-time notifications using WebSocket
- [ ] Invoice PDF upload and OCR processing
- [ ] Advanced analytics and reporting dashboard
- [ ] Mobile app (React Native/Flutter)
- [ ] Integration with accounting software
- [ ] Multi-tenant support
- [ ] Advanced AI features (invoice validation, anomaly detection)
- [ ] Email notifications for approvals and reminders
- [ ] Export to Excel/PDF functionality
- [ ] Advanced search and filtering

## 👤 Author

Developed as a comprehensive full-stack enterprise project demonstrating:
- Spring Boot microservices architecture
- Angular SPA development with modern practices
- Database design and optimization
- Security implementation (JWT, RBAC)
- Business logic and complex workflows
- AI/ML integration with GenAI
- Production-ready code quality

---

## 📝 Documentation Files

- **[README.md](README.md)** - This file (Project overview and setup)
- **[CHATBOT_DOCUMENTATION.md](CHATBOT_DOCUMENTATION.md)** - AI Chatbot detailed documentation
- **[GENAI_SETUP.md](GENAI_SETUP.md)** - GenAI integration setup guide
- **[GENAI_INTEGRATION_SUMMARY.md](GENAI_INTEGRATION_SUMMARY.md)** - GenAI integration summary
- **[DATA_FLOW_EXPLANATION.md](DATA_FLOW_EXPLANATION.md)** - Data flow architecture
- **[QUICK_START.md](QUICK_START.md)** - Quick start guide

---

**Note**: This is a production-ready codebase with proper error handling, validation, security, and documentation. Suitable for portfolio projects, learning advanced Spring Boot and Angular concepts, and demonstrating enterprise-level full-stack development skills.

