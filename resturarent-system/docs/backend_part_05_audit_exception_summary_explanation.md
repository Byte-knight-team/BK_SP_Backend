# Backend Part 05 - Audit Logs Exception Handling and Backend Summary Explanation

**Project:** QR & Online Restaurant Management System
**Member:** Member 01 - System Governance & Access Control
**Backend stack:** Spring Boot, Spring Security, JWT, JPA/Hibernate, MySQL

---

## A. Audit Logs Analysis

### 1. Core Concepts
- **Purpose:** Tracks every critical system action. This allows for security monitoring, debugging, and historical tracking of who changed what, when, and from where.
- **Data Captured:** Module (e.g., RBAC, BRANCH), Event Type (e.g., ROLE_CREATED), Status, Severity, Target ID, Actor ID (who did it), IP Address, HTTP Method, and JSON snapshots of the data before (`oldValuesJson`) and after (`newValuesJson`) the change.
- **Sensitive Data Redaction:** Passwords and tokens are automatically sanitized to `"***"` before saving to the JSON fields.
- **Retrieval:** Highly filterable and paginated endpoint, protected securely for `SUPER_ADMIN` use.

### 2. Detailed File Breakdown

**AuditLog Entity**
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/entity/AuditLog.java`
- **Class name:** `AuditLog`
- **Fields:** `id`, `module`, `eventType`, `status`, `severity`, `targetType`, `description`, `actorUserId`, `branchId`, `oldValuesJson`, `newValuesJson`, `ipAddress`, etc.
- **Database/Data Types:** Enums ensure strict categorization. `LONGTEXT` is used for the JSON snapshots to prevent truncation of large config/role objects. Extensive indexing (`@Index`) on fields like `created_at`, `module`, and `branch_id` ensures that searching millions of rows remains fast.

**AuditLogController & Service**
- **Exact file paths:** 
  - `src/main/java/com/ByteKnights/com/resturarent_system/controller/AuditLogController.java`
  - `src/main/java/com/ByteKnights/com/resturarent_system/service/AuditLogService.java`
- **Important methods:**
  - `logCurrentUserAction(...)`: Automatically pulls the current authenticated user's ID, branch, IP, and Request URI from the Spring Security Context and HTTP Servlet to build the log.
  - `getAuditLogs(...)`: Handles complex dynamic filtering (by date, module, user) using `AuditLogSpecification` and returns paginated data.
  - `sanitizeValue(...)`: Recursively traverses Maps/Collections to mask keys containing words like "password" or "token".
- **Access rules:** `AuditLogController` is heavily restricted via `@PreAuthorize("hasRole('SUPER_ADMIN')")`.

---

## B. Exception Handling Analysis

### 1. Core Concepts
- **Purpose:** Provides a consistent, centralized error response format so the frontend doesn't crash from raw Java stack traces.
- **Global Error Interception:** Uses Spring's `@RestControllerAdvice` to catch exceptions thrown *anywhere* in the application and map them to standard HTTP status codes.

### 2. Detailed File Breakdown

**GlobalExceptionHandler**
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/exception/GlobalExceptionHandler.java`
- **Class name:** `GlobalExceptionHandler`
- **Important methods:** 
  - `@ExceptionHandler(DuplicateResourceException.class)`
  - `@ExceptionHandler(ResourceNotFoundException.class)`
  - `@ExceptionHandler(RuntimeException.class)`
- **Response Format:** Wraps the error message in a standard `ApiResponse.error("message")` body.
- **Code review explanation:** "By using a `GlobalExceptionHandler`, we keep our controllers incredibly clean. We don't need `try-catch` blocks everywhere. When a service throws a `ResourceNotFoundException`, this class catches it and automatically translates it into a standard 404 JSON response for the frontend."

---

## C. Code Review Criteria Check (Audit & Exceptions)

#### 1. Code Formatting
- **Readable and Indented?** Yes. 
- **Files needing cleanup:** None.

#### 2. Naming Conventions
- **Clear names?** Yes. `AuditLog`, `GlobalExceptionHandler`, and `ResourceNotFoundException` are industry standards.

#### 3. Comments and Documentation
- **Useful comments?** Self-documenting code. 

#### 4. No Hardcoding
- **Identified hardcoded values:**
  - `AuditLogController`: Default pagination values (`page = "0"`, `size = "20"`). **Acceptable.**
  - `AuditLogService`: Sensitive key strings (`"password"`, `"token"`). **Acceptable**, but could be moved to application properties if they change frequently.

#### 5. Separation of Concerns
- **Proper separation?** Excellent. The Audit Logic grabs network data directly from `RequestContextHolder`, so the core business services (like `RoleService` or `BranchService`) don't need to know anything about HTTP requests or IPs. They just pass the data.

#### 6. Database/Data Type Understanding
- **Data Types Used:** `LONGTEXT` for JSON fields ensures we don't hit MySQL's standard `VARCHAR(255)` limits when logging large objects. Indexes on search fields optimize querying.

#### 7. Testing Readiness
- **Testing in Postman:**
  - **Get Logs:** GET `/api/admin/audit-logs?page=0&size=10&module=RBAC`.
  - **Trigger Error:** Make a bad request to trigger the GlobalExceptionHandler and observe the `{ "status": "ERROR", "message": "..." }` response.

#### 8. Code Modification Readiness
- **Change 1: "Add a new Audit Module for 'INVENTORY'."**
  - *Modify:* `AuditModule.java` enum. Add `INVENTORY`.
- **Change 2: "Mask 'credit_card' fields in the JSON logs."**
  - *Modify:* `AuditLogService.isSensitiveKey()`. Add `|| normalized.contains("credit_card")`.
- **Change 3: "Change the default error response message for generic 500 errors."**
  - *Modify:* `GlobalExceptionHandler.handleGenericException()`. Change the string inside `ApiResponse.error()`.

#### 9. Error Handling and Validation
- **Errors handled:** Centralized via `@RestControllerAdvice`. It handles custom exceptions (NotFound, Duplicate, Auth) gracefully.

#### 10. Review Explanation
"For monitoring and stability, I implemented an asynchronous Audit Logging system that securely tracks who did what without bloating the business logic. It automatically masks passwords in the JSON snapshots. To ensure our API never breaks the frontend, I centralized all error handling into a `GlobalExceptionHandler`, guaranteeing a uniform JSON structure regardless of where an error occurs."

---

## D. Final Backend Summary

### 1. Complete Backend Endpoint Inventory (Member 01 Scope)
- **Auth:** `POST /api/auth/staff/login`, `POST /api/auth/staff/change-password`
- **Staff Mgmt:** `POST /api/admin/staff`, `PUT /api/admin/staff/{id}`, `PATCH /api/admin/staff/{id}/activate/deactivate`, `GET /api/admin/staff/...`
- **RBAC:** `POST/PUT/GET/DELETE /api/admin/roles`, `PUT /api/admin/roles/{id}/permissions`, `GET /api/admin/privileges`
- **Branch:** `POST/PUT/GET /api/admin/branches`, `PATCH /api/admin/branches/{id}/activate/deactivate`
- **Config:** `GET/PUT /api/admin/config/global`, `GET/PUT /api/admin/config/branches/{id}`, `GET /api/admin/config/branches/{id}/effective`
- **Audit Logs:** `GET /api/admin/audit-logs`, `GET /api/admin/audit-logs/{id}`

### 2. Controller -> Service -> Repository -> Entity Connection
We followed a strict **Layered Architecture**:
1. **Controllers** handle HTTP mapping and Spring Security `@PreAuthorize` role checking.
2. **Services** contain complex validation, cross-repository multi-tenant logic, and audit triggering.
3. **Repositories** are simple Spring Data JPA interfaces handling database communication.
4. **Entities** map exactly to MySQL tables using strict data types (`BigDecimal`, Enums).

### 3. Security, JWT, and RBAC Explanation
- **Stateless Auth:** We use JWT (JSON Web Tokens) signed with an HMAC secret.
- **RBAC Mapping:** Users belong to a `Role`. Roles have many `Privileges` (ManyToMany). When a token is validated, `JwtUserPrincipal` loads the role name, prepends `ROLE_`, and feeds it into Spring Security to power our endpoint annotations.

### 4. Branch Restriction Logic (Multi-tenancy)
The core achievement of Member 01 is Branch Isolation. If an `ADMIN` logs in, the `SystemConfigService` and `StaffService` programmatically look up their `Staff` profile, identify their `Branch ID`, and strictly reject any attempts to edit staff or configurations belonging to a different branch. `SUPER_ADMIN` bypasses this entirely.

### 5. Staff Invite Email System
When an Admin creates a staff member, a 10-character temporary password is created, instantly hashed via BCrypt, saved to the database, and the plain-text string is handed off to an asynchronous `SmtpEmailService` which emails the user. The plaintext is then destroyed from memory. A custom filter (`ForcePasswordChangeFilter`) locks their account until they change it.

### 6. Features Intentionally Excluded
- Database Backup & Export operations were excluded from the core Spring application scope.
- Payment gateway configurations were intentionally ignored as they fall outside current backend testing scope.

### 7. Backend Demo Flow for Project Review
1. Open MySQL Workbench to show the empty `users` and `branches` tables.
2. Log in to Postman as the initial `SUPER_ADMIN`.
3. Create a new `Branch` ("Downtown").
4. Create a new `ADMIN` staff member for the "Downtown" branch. Show the console logging the email sending out.
5. Take the logged temporary password, log in as the new `ADMIN`.
6. Show that the new `ADMIN` gets blocked by the `ForcePasswordChangeFilter` until they hit the `/change-password` endpoint.
7. Attempt to modify the config of a *different* branch to demonstrate the 403 Forbidden branch isolation logic.
8. Call the `GET /api/admin/audit-logs` endpoint to show the system actively tracked all the above actions with masked passwords.

### 8. Backend Testing Checklist
- [x] JWT tokens generate correctly and expire.
- [x] `@PreAuthorize` correctly blocks missing roles.
- [x] Temporary passwords hash immediately.
- [x] Admins are restricted to their own branch configs and staff.
- [x] Global Exception Handler catches all crashes.
- [x] Audit logs record changes without crashing the request.

### 9. What I Should Say During Code Review
*"For Member 01, I implemented the entire security, governance, and infrastructure backbone. I used JWT for stateless authentication and built a dynamic RBAC system where roles and permissions are driven by Many-To-Many database relationships. My proudest implementation is the multi-tenant branch isolation: standard Admins are programmatically locked to their own physical branches, preventing data bleed. To tie it all together, I added an asynchronous Audit Logging system that tracks every payload change, masks passwords automatically, and is protected by a Global Exception Handler to guarantee maximum API uptime."*

### 10. Common Questions Reviewers May Ask
**Q: Why use `BigDecimal` instead of `Double` for delivery fees and taxes?**
A: `Double` suffers from floating-point precision errors. `BigDecimal` is the Java standard for exact decimal calculations, ensuring we don't lose pennies during financial math.

**Q: If I delete a Role, what happens to the users holding that Role?**
A: The system won't let you. The `RoleService` explicitly checks if any active users are assigned to that role and throws an exception, preventing database corruption or user lockouts.

**Q: How do you handle inactive branches?**
A: We use a soft-deactivate approach (changing an Enum status) rather than a hard database delete. This ensures historical orders and staff data tied to that branch remain intact and referentially safe.

---

### End of Part 5 Summary
- **Final list of files analyzed:** `AuditLog`, `AuditLogController`, `AuditLogService`, `GlobalExceptionHandler`, and complete review of all Part 1-4 files.
- **Final list of modules documented:** Authentication, JWT Security, Staff Management, Email Invites, RBAC, Branch Management, System Configuration, Audit Logging, Exception Handling.
- **Missing or unclear backend parts:** None. The backend is fully cohesive and thoroughly documented.
- **Short backend readiness summary:** The backend is 100% ready for review. It demonstrates advanced Spring Security techniques, complex multi-tenant data isolation, robust error handling, and production-ready architectural patterns.
