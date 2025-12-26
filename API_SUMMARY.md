# API Summary - Signature Studio BFF

This document summarizes all the APIs available in the Signature Studio BFF service.

## 📊 Dashboard APIs

### GET `/api/dashboard/stats`
Get dashboard statistics for the logged-in user.

**Query Parameters:**
- `accountId` (optional): Filter statistics by account ID

**Response:** `DashboardStatsDTO`
- `totalTransactions`: Total number of transactions
- `pendingTransactions`: Count of pending transactions
- `completedTransactions`: Count of completed transactions
- `rejectedTransactions`: Count of rejected transactions
- `inProgressTransactions`: Count of in-progress transactions
- `transactionsByStatus`: Map of status to count
- `transactionsByPriority`: Map of priority to count
- `transactionsThisWeek`: Count of transactions created this week
- `completionRate`: Percentage of completed transactions
- `averageProcessingTimeDays`: Average processing time in days

**Authentication:** Required (session-based)

---

## 🔍 Search APIs

### GET `/api/search`
Unified search endpoint for transactions, signers, and IDs.

**Query Parameters:**
- `q` (required): Search query text
- `accountId` (optional): Filter by account ID
- `page` (default: 0): Page number
- `size` (default: 20): Page size

**Response:** `PaginatedResponseDTO<TransactionDTO>`

**Authentication:** Required (session-based)

---

## 🔐 Authentication APIs

### POST `/api/auth/login`
Login endpoint - creates new session with session fixation protection.

**Request Body:** `LoginRequestDTO`
- `username`: Username
- `password`: Password

**Response:** `SessionDTO`

### POST `/api/auth/logout`
Logout endpoint - invalidates session securely.

**Response:** 200 OK

### GET `/api/auth/session`
Get current session information.

**Response:** `SessionDTO`

### GET `/api/auth/csrf-token`
Get CSRF token for frontend.

**Response:** 
```json
{
  "token": "csrf-token-value",
  "headerName": "X-CSRF-TOKEN",
  "parameterName": "_csrf"
}
```

### GET `/api/auth/validate`
Validate if current session is valid.

**Response:**
```json
{
  "valid": true,
  "timestamp": 1234567890
}
```

---

## 📝 Transaction APIs

### GET `/api/transactions/my-transactions`
Get transactions for logged-in user including delegated transactions (with pagination and search).

**Query Parameters:**
- `accountId` (optional): Filter by account ID
- `search` (optional): Search text (searches in title and description)
- `page` (default: 0): Page number
- `size` (default: 20): Page size
- `sortBy` (default: "createdAt"): Field to sort by
- `sortDirection` (default: "desc"): Sort direction (asc/desc)

**Response:** `PaginatedResponseDTO<TransactionDTO>`

### GET `/api/transactions`
Get transactions (legacy endpoint without pagination).

**Query Parameters:**
- `accountId` (optional): Filter by account ID

**Response:** `List<TransactionDTO>`

### GET `/api/transactions/paginated`
Get transactions with pagination and search (without delegation support).

**Query Parameters:**
- `accountId` (optional): Filter by account ID
- `search` (optional): Search text
- `page` (default: 0): Page number
- `size` (default: 20): Page size
- `sortBy` (default: "createdAt"): Field to sort by
- `sortDirection` (default: "desc"): Sort direction

**Response:** `PaginatedResponseDTO<TransactionDTO>`

### POST `/api/transactions`
Create a new transaction.

**Request Body:** `TransactionDTO`

**Response:** `TransactionDTO` (201 Created)

### GET `/api/transactions/{id}`
Get a transaction by ID.

**Response:** `TransactionDTO`

### GET `/api/transactions/{id}/details`
Get full transaction details from ESignatureService (includes documents, form fields, attributes, and ICMP objects).

**Response:** `TransactionDTO`

### PUT `/api/transactions/{id}`
Update a transaction.

**Request Body:** `TransactionDTO`

**Response:** `TransactionDTO`

### DELETE `/api/transactions/{id}`
Delete a transaction.

**Response:** 204 No Content

### GET `/api/transactions/{id}/status`
Get transaction status.

**Response:**
```json
{
  "status": "pending",
  "transactionId": "transaction-id"
}
```

---

## 📄 Document APIs

### GET `/api/transactions/{transactionId}/documents`
Get all documents for a transaction.

**Response:** `List<DocumentDTO>`

### POST `/api/transactions/{transactionId}/documents`
Upload a document (multipart/form-data).

**Form Data:**
- `file`: The document file
- `formFields` (optional): JSON string of form fields

**Response:** `DocumentDTO` (201 Created)

### GET `/api/transactions/{transactionId}/documents/{documentId}`
Get a specific document.

**Response:** `DocumentDTO`

### GET `/api/transactions/{transactionId}/documents/{documentId}/details`
Get full document details from ESignatureService (includes form fields and ICMP objects).

**Response:** `DocumentDTO`

### PUT `/api/transactions/{transactionId}/documents/{documentId}`
Update a document.

**Request Body:** `DocumentDTO`

**Response:** `DocumentDTO`

### DELETE `/api/transactions/{transactionId}/documents/{documentId}`
Delete a document.

**Response:** 204 No Content

### GET `/api/transactions/{transactionId}/documents/{documentId}/form-fields`
Get form fields for a document.

**Response:** `List<FormFieldDTO>`

### PUT `/api/transactions/{transactionId}/documents/{documentId}/form-fields`
Update form fields for a document.

**Request Body:** `List<FormFieldDTO>`

**Response:** `DocumentDTO`

### GET `/api/transactions/{transactionId}/documents/{documentId}/download`
Download a document (redirects to ESignatureService).

**Response:** Redirect or 501 Not Implemented

---

## 👥 User/Signer APIs

### GET `/api/transactions/{transactionId}/users`
Get all users (signers) for a transaction.

**Response:** `List<UserDTO>`

### POST `/api/transactions/{transactionId}/users`
Add a user (signer) to a transaction.

**Request Body:** `AddUserRequest`

**Response:** `UserDTO` (201 Created)

### PUT `/api/transactions/{transactionId}/users/{userId}`
Update a user (signer).

**Request Body:** `UserDTO`

**Response:** `UserDTO`

### DELETE `/api/transactions/{transactionId}/users/{userId}`
Delete a user (signer) from a transaction.

**Response:** 204 No Content

---

## 🏢 Account APIs

### GET `/api/accounts`
Get all accounts that the current user has access to.

**Response:** `List<AccountDTO>`

### GET `/api/accounts/current`
Get the current account context from session.

**Response:** `AccountDTO`

### POST `/api/accounts/switch`
Switch to a different account.

**Request Body:** `SwitchAccountRequestDTO`
- `accountId`: The account ID to switch to

**Response:** `SessionDTO`

### GET `/api/accounts/{accountId}/validate`
Validate if user has access to a specific account.

**Response:**
```json
{
  "accountId": "account-id",
  "hasAccess": true
}
```

---

## 🎨 Branding APIs

### GET `/api/branding/account/{accountId}`
Get branding by account ID.

**Response:** `BrandingDTO`

### GET `/api/branding/account-code/{code}`
Get branding by account code.

**Response:** `BrandingDTO`

### PUT `/api/branding/account/{accountId}`
Save branding for an account.

**Request Body:** `BrandingDTO`

**Response:** 200 OK

---

## 📧 Alert APIs

### POST `/api/alerts/send`
Send an alert (email).

**Request Body:** `AlertRequestDTO`
- `templateId`: Email template ID
- `recipientEmail`: Recipient email address
- `recipientName`: Recipient name
- `transactionId`: Transaction ID

**Response:** 200 OK

### GET `/api/alerts/templates`
Get all email templates.

**Response:** `List<EmailTemplateDTO>`

### GET `/api/alerts/templates/{id}`
Get a specific email template.

**Response:** `EmailTemplateDTO`

---

## ⚙️ Admin Panel APIs

### GET `/api/admin/settings/account`
Get account settings for the current account.

**Query Parameters:**
- `accountId` (optional): Account ID (defaults to session account)

**Response:** `AccountSettingsDTO`
- `companyName`: Company name
- `defaultDueDays`: Default due days for transactions
- `requireAuthentication`: Whether authentication is required
- `allowDelegation`: Whether delegation is allowed
- `autoArchive`: Whether to auto-archive
- `retentionDays`: Retention period in days

### PUT `/api/admin/settings/account`
Update account settings.

**Query Parameters:**
- `accountId` (optional): Account ID (defaults to session account)

**Request Body:** `AccountSettingsDTO`

**Response:** 200 OK

### GET `/api/admin/settings/notifications`
Get notification settings for the current account.

**Query Parameters:**
- `accountId` (optional): Account ID (defaults to session account)

**Response:** `NotificationSettingsDTO`
- `emailNotifications`: Enable email notifications
- `smsNotifications`: Enable SMS notifications
- `reminderFrequency`: Reminder frequency (hourly, daily, weekly)
- `escalationEnabled`: Whether escalation is enabled
- `escalationDays`: Days before escalation

### PUT `/api/admin/settings/notifications`
Update notification settings.

**Query Parameters:**
- `accountId` (optional): Account ID (defaults to session account)

**Request Body:** `NotificationSettingsDTO`

**Response:** 200 OK

### GET `/api/admin/templates`
Get all email templates (same as `/api/alerts/templates`).

**Response:** `List<EmailTemplateDTO>`

### GET `/api/admin/templates/{id}`
Get a specific email template (same as `/api/alerts/templates/{id}`).

**Response:** `EmailTemplateDTO`

### POST `/api/admin/templates`
Create a new email template.

**Request Body:** `EmailTemplateDTO`
- `name`: Template name
- `subject`: Email subject
- `body`: Email body
- `accountId`: Account ID
- `variables`: Map of template variables

**Response:** `EmailTemplateDTO` (201 Created)

### PUT `/api/admin/templates/{id}`
Update an email template.

**Request Body:** `EmailTemplateDTO`

**Response:** 200 OK

### DELETE `/api/admin/templates/{id}`
Delete an email template.

**Response:** 200 OK

---

## 🔄 Server-Sent Events (SSE) APIs

### GET `/api/sse/events`
Subscribe to general events stream (text/event-stream).

**Response:** SSE stream with real-time updates

**Events:**
- `connected`: Initial connection event
- `transaction-update`: Transaction status updates

**Authentication:** Required (session-based)

### GET `/api/sse/transactions/{transactionId}/updates`
Subscribe to transaction-specific events stream.

**Response:** SSE stream with real-time updates for the specified transaction

**Events:**
- `connected`: Initial connection event
- `transaction-update`: Transaction status updates

**Authentication:** Required (session-based)

---

## 👥 Team Member APIs

### GET `/api/team-members/search`
Search for team members (LDAP integration).

**Query Parameters:**
- `query`: Search query
- `accountId` (optional): Filter by account ID

**Response:** `List<TeamMemberDTO>`

---

## 🔄 Delegation APIs

### GET `/api/delegations`
Get delegations for the current user.

**Query Parameters:**
- `type` (optional): Filter by type (delegator/delegate)

**Response:** `List<DelegationDTO>`

### POST `/api/delegations`
Create a new delegation.

**Request Body:** `DelegationDTO`

**Response:** `DelegationDTO` (201 Created)

### PUT `/api/delegations/{id}`
Update a delegation.

**Request Body:** `DelegationDTO`

**Response:** `DelegationDTO`

### POST `/api/delegations/{id}/cancel`
Cancel a delegation.

**Response:** 200 OK

---

## 📝 Notes

1. **Authentication**: Most endpoints require a valid session. Session is managed via Spring Session stored in Oracle DB.

2. **CSRF Protection**: POST/PUT/DELETE requests require CSRF token. Get token from `/api/auth/csrf-token`.

3. **Pagination**: Paginated endpoints use `PaginatedResponseDTO` with:
   - `content`: List of items
   - `totalElements`: Total count
   - `totalPages`: Total pages
   - `page`: Current page (0-indexed)
   - `size`: Page size

4. **Delegation Support**: Transaction queries automatically include delegated transactions when user is a delegate.

5. **Account Filtering**: Most endpoints support optional `accountId` parameter to filter by account.

6. **Error Handling**: All endpoints return appropriate HTTP status codes and `ErrorResponseDTO` for errors.

