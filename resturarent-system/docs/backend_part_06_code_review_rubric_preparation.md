# Backend Part 06 - Code Review Rubric Preparation

**Project:** QR & Online Restaurant Management System
**Member:** Member 01 - System Governance & Access Control
**Backend stack:** Spring Boot, Spring Security, JWT, JPA/Hibernate, MySQL

---

## 1. Code Formatting - 3 Marks
- **Current Estimated Status:** Excellent (3/3)
- **Clean Files:** The entire backend follows a highly consistent structural standard. Files like `BranchService.java` and `RoleService.java` use clean indentation, structured imports, and clear logical separation via blank lines.
- **Files needing cleanup:** None strictly require formatting cleanup. However, the `StaffService.java` is exceptionally long (>800 lines). A reviewer might suggest moving basic validation rules into a generic `ValidationUtils` class.
- **Readability:** Indentation, spacing, and bracket placement strictly adhere to standard Java conventions. Method signatures are readable, and annotations are stacked clearly above fields and methods.

## 2. Naming Conventions - 3 Marks
- **Current Estimated Status:** Excellent (3/3)
- **Class Names:** Clear and standard (e.g., `AuditLogController`, `SystemConfigService`).
- **Method Names:** Very descriptive action verbs (e.g., `resolveTargetBranch`, `validateAdminBranchWriteAccess`).
- **DTO Names:** Accurately convey payload intent (e.g., `UpdateBranchConfigRequest`, `EffectiveBranchConfigResponse`).
- **Repository Names:** Standard Spring Data JPA conventions (e.g., `UserRepository`).
- **Entity Names:** Singular nouns representing tables accurately (e.g., `Privilege`, `BranchOperatingHours`).
- **Package Names:** Standard lowercase dot-separated (`com.ByteKnights.com.resturarent_system.security`).
- **Endpoint Naming:** Clean RESTful nouns (e.g., `/api/admin/staff`, `/api/admin/roles/{roleId}/permissions`).
- **Inconsistencies:** None significant. The naming convention is one of the strongest parts of the codebase.

## 3. Comments and Documentation - 3 Marks
- **Current Estimated Status:** Good (2.5/3)
- **Meaningful Comments:** There are helpful comments inside `EmailTemplateService` explaining the `localhost` URL, and in JWT classes.
- **Unnecessary Commented-Out Code:** None found.
- **Suggestions for Improvement:** 
  - `StaffService.resolveTargetBranch()` contains complex logic differentiating `SUPER_ADMIN` and `ADMIN` flows. Adding inline comments here would help explain the logic instantly during a review.
  - `RoleService.assignPermissionsToRole()` should have a comment noting that it *replaces* existing permissions rather than *appending* them.

## 4. No Hardcoding - 3 Marks
- **Current Estimated Status:** Good (2.5/3)
- **a. Acceptable hardcoding:**
  - `AuditLogService`: Default page size (`size = 20`).
  - `RoleService`: The `CORE_ROLES` Set containing strings like `"SUPER_ADMIN"`. (Acceptable for system stability, though an Enum could be safer).
  - Default config values in `SystemConfig.java` (e.g., `BigDecimal.ZERO`).
- **b. Should be moved to constants:**
  - Status strings and Error messages (e.g., `"Branch not found"`, `"Email already exists"`) could be moved to a final `ErrorMessages` class to prevent typos.
- **c. Should be moved to application.properties / environment variables:**
  - `JwtService.java`: The JWT Secret Key and Expiration times MUST be moved to `application.properties` (e.g., `${jwt.secret}`). This is a common reviewer target.
  - `EmailTemplateService.java`: The frontend login URL (`http://localhost:5173/staff/login`) MUST be moved to properties so it can change dynamically in production.

## 5. Separation of Concern - 3 Marks
- **Current Estimated Status:** Excellent (3/3)
- **Separation Check:** Flawless implementation of the Layered Architecture. Controllers only handle HTTP mappings and DTO routing. Services handle business logic and database orchestration. Entities purely map data. Repositories purely interface with the DB. Security logic is cleanly isolated in filters (`JwtAuthenticationFilter`, `ForcePasswordChangeFilter`).
- **Overloaded Classes:** As mentioned, `StaffService.java` handles DTO mapping, HTTP validation, branching logic, password hashing, email triggering, and audit logging. Moving email triggering and validation to separate utility services could reduce its responsibility.

## 6. Code/Database Contribution - 25 Marks
- **Summary of Contribution:** Built the entire foundational governance, security, and multi-tenant infrastructure. Without Member 01's work, the rest of the application (Orders, Menu, Cart) cannot identify users, isolate branch data, or secure endpoints.
- **Authentication/Security:** Crucial for preventing unauthorized access. Stateless JWT allows the backend to scale without session memory overhead.
- **Staff Management:** Essential for restaurant operations. The automated invite email and hashed temporary password flow ensure immediate secure onboarding.
- **RBAC (Roles-Based Access Control):** Important because a Chef should not be able to delete a Branch, and a Receptionist should not modify global taxes.
- **Branch Management:** Critical for multi-tenancy. Restricting standard `ADMIN`s to their own branch prevents data cross-contamination between physical locations.
- **System Configuration:** Important because it allows dynamic adjustments to taxes, delivery fees, and operating hours without touching code.
- **Audit Logs:** Essential for accountability. If taxes are changed maliciously, the audit log instantly points to the exact user, IP, and time.
- **Database Tables Created/Used:** `users`, `staff`, `roles`, `privileges`, `role_permissions` (join table), `branches`, `system_config`, `branch_config`, `branch_operating_hours`, `audit_logs`.

## 7. Knowledge Regarding Code Contribution - 15 Marks
**Reviewer Questions & Answers:**
1. **Q: How does JWT work in this system?**
   **A:** Upon login, we verify the hashed password, then create a stateless JSON string containing the user's ID, role, and branch, signed with an HMAC secret. The client sends this token in the `Authorization: Bearer` header on subsequent requests.
2. **Q: How does Spring Security protect endpoints?**
   **A:** `SecurityConfig` defines broad route access. Our `JwtAuthenticationFilter` intercepts requests, validates the token, and creates a `JwtUserPrincipal`. We use `@PreAuthorize("hasRole(...)")` on controllers, which checks the role loaded into the principal.
3. **Q: Why don't we store the temporary password?**
   **A:** Storing plaintext passwords is a severe security flaw. We generate it, immediately hash it with BCrypt, save the hash, and send the plaintext to the email service before discarding it from memory.
4. **Q: What is the difference between a User and Staff?**
   **A:** `User` handles generic auth credentials (email, hashed password, role). `Staff` handles restaurant-specific data (employment status, assigned branch) and links to `User` via a Foreign Key.
5. **Q: How does Branch Restriction work for Admins?**
   **A:** When an Admin requests to edit staff or config, the service checks their `Staff` entity to find their assigned `Branch ID`. If the requested action targets a different Branch ID, we throw an exception. `SUPER_ADMIN` bypasses this check.
6. **Q: Why do we use DTOs?**
   **A:** To decouple our database Entities from our API responses. This prevents infinite recursion errors, hides sensitive fields (like passwords), and prevents over-posting attacks.
7. **Q: How does the system handle errors?**
   **A:** We use a `@RestControllerAdvice` called `GlobalExceptionHandler` to intercept all exceptions globally and translate them into a uniform JSON response, ensuring the frontend never crashes from a raw stack trace.

## 8. Testing - 15 Marks
**Backend Testing Checklist (Postman):**
- [ ] **Successful login:** `POST /api/auth/staff/login` with correct credentials -> Returns JWT and Branch Info.
- [ ] **Failed login:** Incorrect password -> Returns 401 Unauthorized.
- [ ] **Protected endpoint without token:** `GET /api/admin/roles` -> Returns 403/401.
- [ ] **Protected endpoint with invalid token:** Alter a character in the token -> Returns 401.
- [ ] **Staff create:** `SUPER_ADMIN` creates staff -> Returns 200, temporary password hashed, email sent.
- [ ] **Duplicate email test:** Create staff with existing email -> Returns duplicate error gracefully.
- [ ] **Change Password filter test:** Log in with temporary password, try hitting `GET /api/admin/staff` -> Returns `Must change password`.
- [ ] **Branch activate/deactivate test:** Toggle branch status -> Status changes in DB.
- [ ] **ADMIN branch restriction test:** Log in as `ADMIN` (Branch 1), try to PUT config for Branch 2 -> Returns 403/Error.
- [ ] **Audit log filter test:** `GET /api/admin/audit-logs?module=RBAC` -> Returns only RBAC logs.

*Future Unit Tests to Add:* MockMVC tests for the `StaffController` to verify branch isolation, and Mockito tests for `StaffService` to verify the email service is called exactly once upon staff creation.

## 9. Code Modification - 15 Marks
**Live Modification Tasks to Prepare For:**

1. **"Add a new privilege (e.g., 'MANAGE_INVENTORY')."**
   - **Edit:** Just POST to the DB if a generic endpoint existed, but since we rely on Roles currently, you'd add the Privilege to the database directly and assign it to a Role via `PUT /api/admin/roles/{id}/permissions`.
2. **"Change the temporary password from 10 to 12 characters."**
   - **Edit:** `StaffService.java` -> Locate `generateTempPassword()` and change `int length = 10;` to `12`.
   - **Test:** Create a new staff member and count the characters in the email payload.
3. **"Prevent SUPER_ADMIN from renaming the 'CHEF' role."**
   - **Edit:** `RoleService.java` -> Ensure `"CHEF"` is inside the `CORE_ROLES` Set. The logic to prevent renaming core roles already exists!
4. **"Change the frontend login URL in the invite email."**
   - **Edit:** `EmailTemplateService.java` -> Modify the `loginUrl` string.
   - **Test:** Resend an invite and check the email output.
5. **"Add a 'Packaging Fee' to Branch Configuration."**
   - **Edit:** `BranchConfig.java` (add field), `UpdateBranchConfigRequest`, `BranchConfigResponse`, and map it in `SystemConfigService.java`.
6. **"Mask 'contact_number' in the Audit Logs."**
   - **Edit:** `AuditLogService.java` -> Locate `isSensitiveKey()` and add `|| normalized.contains("contact_number")`.

## 10. Error Handling and Validation - 15 Marks
- **Validations Existing:** 
  - Regex checks for Email (`^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$`).
  - Regex checks for Phone numbers (10 digits/international).
  - Mathematical bounds checking for Taxes/Fees (must be between 0 and 100).
  - Duplicate uniqueness checks against the database for emails, phone numbers, and branch names.
- **Global Error Handling:** Handled flawlessly via `GlobalExceptionHandler` intercepting `RuntimeException`, `ResourceNotFoundException`, etc., and formatting them into `ApiResponse.error()`.
- **System Stability:** Because of the Global Exception Handler and DTO validation layers, it is virtually impossible to crash the server with bad JSON input; the user will always receive a 400 Bad Request instead of causing a 500 Internal Server Error.

---

## 🚀 Final Review Preparation Strategy

**1. Backend Strengths (Mention these!)**
- Extremely robust multi-tenant branch isolation security.
- Bulletproof `GlobalExceptionHandler` ensuring API stability.
- Automated Audit Logging that masks sensitive passwords without bloating business logic.

**2. Backend Weak Points (Fix these before review if possible!)**
- Move JWT Secrets and Frontend URLs out of hardcoded strings into `application.properties`.
- Add `@Valid` annotations to controller DTOs to utilize Spring Boot's built-in validation engine instead of manual `if` checks.

**3. Top 10 Questions You MUST Be Ready to Answer**
1. Explain the flow of a JWT token from generation to validation.
2. Why is the temporary password hashed instantly?
3. How do you prevent an Admin from Branch A editing staff in Branch B?
4. What is the difference between `@PreAuthorize("hasRole()")` and checking a Privilege?
5. Why did you choose `BigDecimal` for configuration percentages?
6. Explain the purpose of a DTO.
7. How does the `GlobalExceptionHandler` work under the hood?
8. Explain the `ForcePasswordChangeFilter` logic.
9. How do you safely delete a Role? (Answer: You can't if it's assigned to users or is a core role).
10. What happens if the SMTP server fails when creating staff? (Answer: It's caught, and the DB status is marked `FAILED` so we can resend later).

**4. Top 10 Files You MUST Understand Deeply**
1. `StaffService.java` (Branch validation and creation flow)
2. `SecurityConfig.java` (Route definitions)
3. `JwtAuthenticationFilter.java` (Token interception)
4. `ForcePasswordChangeFilter.java` (Security lockouts)
5. `RoleService.java` (Core role protection)
6. `SystemConfigService.java` (Effective config merging)
7. `AuditLogService.java` (Request context harvesting and masking)
8. `GlobalExceptionHandler.java` (Error mapping)
9. `JwtUserPrincipal.java` (Spring Security authority bridging)
10. `AuthService.java` (Login and BCrypt matching)

**5. Backend Final Readiness Score Estimate**
**95/100.** The architecture is highly advanced, secure, and logically sound. The only deductions are minor hardcoded properties and slightly verbose manual validations.

**6. What to Practice Explaining Before Review**
Practice opening Postman, logging in as a `SUPER_ADMIN`, creating an `ADMIN` for a specific branch, and then logging in as that `ADMIN` to demonstrate that you receive a 403 Forbidden error when trying to edit a different branch's configuration. This perfectly demonstrates your mastery of Authentication, JWTs, RBAC, and Branch Management all in one flow.
