# BFF Service - Project Context Documentation

> **Purpose**: This document provides context for AI assistants and developers working on the Spring Boot BFF service. It captures architectural decisions, terminology, and key patterns.

## 🎯 Project Overview

**Service Name**: Wells Fargo eSignature BFF (Backend for Frontend)  
**Technology**: Spring Boot 3.x  
**Purpose**: Acts as a BFF layer between the React UI and backend services, handling session management, security, and transaction metadata.

## 📋 Key Terminology

### ⚠️ IMPORTANT: Recipient Signers (NOT Recipients)
- **Always use "Recipient Signers" or "Signers" in code, APIs, and documentation**
- The UI uses "Recipients" but the backend should use "Recipient Signers"
- Database tables: `recipient_signers` (not `recipients`)
- Entity class: `RecipientSigner` (not `Recipient`)
- API endpoints: `/api/recipient-signers` (not `/api/recipients`)

**Rationale**: The term "Recipient Signers" is more accurate as these are people who will sign documents, not just recipients of notifications.

## 🗄️ Database Configuration

### Oracle Database
- **Primary DB**: Oracle Database (for transaction metadata and Spring Session)
- **Session Store**: Oracle Database (using Spring Session JDBC)
- **Connection Pool**: HikariCP

### Spring Session with Oracle
```yaml
spring:
  session:
    store-type: jdbc
    jdbc:
      initialize-schema: always
      table-name: SPRING_SESSION
  datasource:
    url: jdbc:oracle:thin:@${DB_HOST:localhost}:${DB_PORT:1521}/${DB_SERVICE:ORCL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: oracle.jdbc.OracleDriver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

### Required Oracle Dependencies
```xml
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-jdbc</artifactId>
</dependency>
```

## 🏗️ Architecture

### Service Architecture
```
React UI (Frontend)
    ↓ HTTP/REST + SSE
BFF Service (Spring Boot)
    ├── Session Management (Oracle DB)
    ├── CSRF Protection
    ├── Authentication (Ping - mocked)
    ├── Transaction Metadata (Oracle DB)
    └── Service Clients
        ├── eSignature Backend Service
        ├── Alert Service (Email templates & sending)
        └── Branding Service
```

### Key Responsibilities
1. **Session Management**: Store user sessions in Oracle DB using Spring Session
2. **Security**: CSRF protection, authentication, authorization
3. **Transaction Metadata**: Store transaction info (who created, status, account info)
4. **Service Orchestration**: Coordinate calls to eSignature, Alert, and Branding services
5. **SSE/MCP Tools**: Real-time updates via Server-Sent Events

## 📊 Database Schema

### Transaction Metadata Table
```sql
CREATE TABLE transaction_metadata (
    id VARCHAR2(255) PRIMARY KEY,
    title VARCHAR2(500),
    description CLOB,
    status VARCHAR2(50), -- pending, in-progress, completed, rejected
    created_by VARCHAR2(255), -- User ID/Email
    account_id VARCHAR2(255),
    account_code VARCHAR2(255),
    esignature_transaction_id VARCHAR2(255), -- From eSignature backend
    document_url VARCHAR2(1000),
    due_date TIMESTAMP,
    priority VARCHAR2(20), -- low, medium, high
    email_template VARCHAR2(100),
    system_of_record VARCHAR2(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transaction_account ON transaction_metadata(account_id);
CREATE INDEX idx_transaction_created_by ON transaction_metadata(created_by);
CREATE INDEX idx_transaction_status ON transaction_metadata(status);
```

### Recipient Signers Table (NOT Recipients)
```sql
CREATE TABLE recipient_signers (
    id VARCHAR2(255) PRIMARY KEY,
    transaction_id VARCHAR2(255) NOT NULL,
    name VARCHAR2(255),
    email VARCHAR2(255),
    unique_id VARCHAR2(255),
    role VARCHAR2(50), -- signer, reviewer, approver
    signing_order NUMBER(10), -- Order for signers
    type VARCHAR2(50), -- team-member, customer
    FOREIGN KEY (transaction_id) REFERENCES transaction_metadata(id) ON DELETE CASCADE
);

CREATE INDEX idx_recipient_signers_transaction ON recipient_signers(transaction_id);
CREATE INDEX idx_recipient_signers_email ON recipient_signers(email);
```

### Transaction Attributes Table
```sql
CREATE TABLE transaction_attributes (
    transaction_id VARCHAR2(255),
    attribute_key VARCHAR2(255),
    attribute_value CLOB,
    PRIMARY KEY (transaction_id, attribute_key),
    FOREIGN KEY (transaction_id) REFERENCES transaction_metadata(id) ON DELETE CASCADE
);
```

### ICMP Document Relationships
```sql
CREATE TABLE icmp_relationships (
    id VARCHAR2(255) PRIMARY KEY,
    transaction_id VARCHAR2(255) NOT NULL,
    relationship_type VARCHAR2(10), -- ACCT, CUST
    account_number VARCHAR2(255),
    system_of_record VARCHAR2(100),
    customer_recipient_signer_id VARCHAR2(255), -- References recipient_signers.id
    FOREIGN KEY (transaction_id) REFERENCES transaction_metadata(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_recipient_signer_id) REFERENCES recipient_signers(id) ON DELETE SET NULL
);
```

### Spring Session Tables (Auto-created)
- `SPRING_SESSION` - Session data
- `SPRING_SESSION_ATTRIBUTES` - Session attributes

## 🔐 Security Configuration

### CSRF Protection
- **Strategy**: Cookie-based CSRF tokens
- **Cookie Name**: `XSRF-TOKEN`
- **Header Name**: `X-XSRF-TOKEN`
- **Exclusions**: `/api/public/**`, `/api/auth/login`

### Session Management
- **Store**: Oracle Database (Spring Session JDBC)
- **Timeout**: 30 minutes
- **Max Sessions**: 1 per user
- **Session Fixation**: Change session ID on login

### Authentication
- **Current**: Mock Ping authentication
- **Future**: Ping Identity OAuth2/OIDC integration
- **Session Storage**: Oracle DB via Spring Session

## 🔌 Service Integrations

### eSignature Backend Service
**Purpose**: Core eSignature transaction processing  
**Base URL**: `${ESIGNATURE_SERVICE_URL}`

**Key Endpoints**:
- `POST /api/v1/transactions` - Create transaction
- `GET /api/v1/transactions/{id}` - Get transaction status
- `PUT /api/v1/transactions/{id}/fields` - Update fields
- `GET /api/v1/transactions/{id}/status` - Get status

### Alert Service
**Purpose**: Email template management and sending  
**Base URL**: `${ALERT_SERVICE_URL}`

**Key Endpoints**:
- `POST /api/v1/alerts/send` - Send email alert
- `GET /api/v1/templates` - Get email templates
- `PUT /api/v1/templates/{id}` - Update template

### Branding Service
**Purpose**: Account-specific branding configurations  
**Base URL**: `${BRANDING_SERVICE_URL}`

**Key Endpoints**:
- `GET /api/v1/branding/account/{accountId}` - Get branding
- `PUT /api/v1/branding/account/{accountId}` - Save branding
- `GET /api/v1/branding/account-code/{code}` - Get by account code

## 📡 API Endpoints

### Authentication
```
POST   /api/auth/login              # Authenticate (mock Ping)
POST   /api/auth/logout             # Logout
GET    /api/auth/session            # Get current session
GET    /api/auth/csrf-token         # Get CSRF token
```

### Transactions
```
GET    /api/transactions                    # List transactions (filtered by account/user)
POST   /api/transactions                    # Create transaction
GET    /api/transactions/{id}               # Get transaction details
PUT    /api/transactions/{id}               # Update transaction
DELETE /api/transactions/{id}               # Delete transaction
GET    /api/transactions/{id}/status        # Get status from eSignature service
```

### Recipient Signers (NOT Recipients)
```
GET    /api/transactions/{id}/recipient-signers     # Get signers for transaction
POST   /api/transactions/{id}/recipient-signers     # Add signer
PUT    /api/transactions/{id}/recipient-signers/{signerId}  # Update signer
DELETE /api/transactions/{id}/recipient-signers/{signerId}  # Remove signer
```

### Branding
```
GET    /api/branding/account/{accountId}    # Get branding for account
PUT    /api/branding/account/{accountId}    # Save branding
GET    /api/branding/account-code/{code}    # Get branding by account code
```

### Alerts
```
POST   /api/alerts/send                     # Send email alert
GET    /api/alerts/templates                # Get email templates
GET    /api/alerts/templates/{id}           # Get template details
```

### SSE (Server-Sent Events)
```
GET    /api/sse/events                      # SSE stream for real-time updates
GET    /api/sse/transactions/{id}/updates   # Transaction-specific updates
```

## 📦 Key Dependencies

```xml
<!-- Spring Boot Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Spring Session JDBC (Oracle) -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-jdbc</artifactId>
</dependency>

<!-- Oracle JDBC -->
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- WebFlux for SSE -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

## 🎨 Entity Models

### Transaction Entity
```java
@Entity
@Table(name = "transaction_metadata")
public class Transaction {
    @Id
    private String id;
    
    private String title;
    private String description;
    private String status;
    private String createdBy;
    private String accountId;
    private String accountCode;
    private String eSignatureTransactionId;
    private String documentUrl;
    
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    private List<RecipientSigner> recipientSigners; // NOT recipients
    
    @ElementCollection
    @CollectionTable(name = "transaction_attributes")
    private Map<String, String> customAttributes;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### RecipientSigner Entity (NOT Recipient)
```java
@Entity
@Table(name = "recipient_signers")
public class RecipientSigner {
    @Id
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    
    private String name;
    private String email;
    private String uniqueId;
    private String role; // signer, reviewer, approver
    private Integer signingOrder;
    private String type; // team-member, customer
}
```

## 🔄 Data Flow

### Transaction Creation Flow
1. UI sends transaction data to BFF
2. BFF validates and stores metadata in Oracle DB
3. BFF calls eSignature Backend Service to create transaction
4. BFF stores eSignature transaction ID in metadata
5. BFF calls Alert Service to send initial email
6. BFF returns transaction ID to UI

### Status Update Flow
1. UI requests transaction status
2. BFF checks local metadata first (fast)
3. BFF optionally calls eSignature service for latest status
4. BFF updates local metadata if changed
5. BFF returns status to UI

## 🚀 Development Guidelines

### Naming Conventions
- **Always use "RecipientSigner" or "Signer"** - never "Recipient" in backend code
- Use camelCase for Java variables
- Use UPPER_SNAKE_CASE for constants
- Use PascalCase for class names

### Code Organization
```
com.wellsfargo.bff/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── service/         # Business logic
├── repository/      # Data access (JPA)
├── model/           # Entities and DTOs
├── security/        # Security-related classes
└── exception/       # Exception handlers
```

### Best Practices
1. Always validate input in controllers
2. Use DTOs for API requests/responses (not entities directly)
3. Handle service integration failures gracefully
4. Log all transaction state changes
5. Use transactions for multi-step operations
6. Cache branding data (consider Redis for future)

## 🔍 Key Design Decisions

1. **Why store metadata in BFF?**
   - Fast queries for dashboard/list views
   - Audit trail (who created, when)
   - Status tracking without calling backend
   - Account-based filtering
   - Can sync with eSignature backend asynchronously

2. **Why Oracle for Spring Session?**
   - Enterprise standard
   - Consistent with transaction metadata storage
   - No additional infrastructure needed
   - Spring Session JDBC supports Oracle

3. **Why "Recipient Signers" terminology?**
   - More accurate: these are people who sign documents
   - Distinguishes from notification recipients
   - Clearer intent in code and APIs

## 📝 Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=1521
DB_SERVICE=ORCL
DB_USERNAME=wfs_bff_user
DB_PASSWORD=your_password

# Service URLs
ESIGNATURE_SERVICE_URL=http://localhost:8081
ALERT_SERVICE_URL=http://localhost:8082
BRANDING_SERVICE_URL=http://localhost:8083

# Application
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

## 🧪 Testing Considerations

- Use H2 in-memory DB for unit tests
- Use Testcontainers with Oracle for integration tests
- Mock service clients in unit tests
- Test CSRF token handling
- Test session expiration
- Test concurrent session limits

## 📚 Additional Resources

- Spring Session JDBC Documentation: https://docs.spring.io/spring-session/reference/jdbc.html
- Oracle JDBC Driver: https://www.oracle.com/database/technologies/appdev/jdbc.html
- Spring Security CSRF: https://docs.spring.io/spring-security/reference/features/exploits/csrf.html

---

**Last Updated**: 2024-01-XX  
**Maintained By**: Wells Fargo eSignature Team

