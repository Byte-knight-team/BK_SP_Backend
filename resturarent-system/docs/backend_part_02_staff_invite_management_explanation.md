# Backend Part 02 - Staff Invite and Staff Management Explanation

**Project:** QR & Online Restaurant Management System
**Member:** Member 01 - System Governance & Access Control
**Backend stack:** Spring Boot, Spring Security, JWT, JPA/Hibernate, MySQL

---

## PART 2: Staff Account Creation, Invite Email Flow, and Staff Management

### Flow Diagrams

**Create Staff Flow:**
```text
StaffController
-> StaffService
-> RoleRepository (Validate role)
-> BranchRepository (Validate branch)
-> PasswordEncoder (Hash temp password)
-> UserRepository (Save User)
-> StaffRepository (Save Staff profile)
-> EmailService (Send invite)
-> Response DTO
```

**Staff Invite Email Flow:**
```text
Create staff request
-> Generate temporary password
-> Encode password before saving
-> Build invite email body
-> Send email to staff
-> Return response
```

**Resend Invite Flow:**
```text
Controller
-> Service
-> Find user/staff
-> Generate new temp password
-> Encode password
-> Send invite email
-> Save updated user
```

**Staff Update Flow:**
```text
Controller
-> Service
-> Validate requester role
-> Validate branch permission
-> Validate duplicate email/phone
-> Update user/staff details
-> Save changes
```

**Staff Activate/Deactivate Flow:**
```text
Controller
-> Service
-> Find user
-> Validate permission
-> Change active status
-> Save user
```

---

### Detailed File Analysis

#### 1. StaffController
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/controller/StaffController.java`
- **Class name:** `StaffController`
- **Purpose:** Exposes all HTTP endpoints related to staff management.
- **Why we use it:** To provide a REST API interface for the frontend to manage staff.
- **Important methods/functions:** 
  - `createStaff`, `resendInvite`, `getAllStaff`, `getStaffById`, `updateStaff`, `activateStaff`, `deactivateStaff`, `getStaffByBranch`, `getStaffByRole`.
- **Input DTO:** `CreateStaffRequest`, `UpdateStaffRequest`.
- **Response DTO:** `StaffResponse`, `CreateStaffResponse`.
- **Step-by-step logic:** Intercepts incoming HTTP requests, applies method-level security (`@PreAuthorize`), and forwards the payload to `StaffService`.
- **What other files it calls:** `StaffService`.
- **Database tables/entities involved:** None directly.
- **Validation rules:** Relies on Spring Security for role checking (`hasAnyRole('SUPER_ADMIN','ADMIN')`).
- **Error cases:** Access Denied (403) if a user without the proper role attempts to access.
- **Access rules:** All endpoints are strictly protected to `SUPER_ADMIN` and `ADMIN`.
- **Code review explanation:** "This is the entry point for staff management operations. It ensures only authenticated administrators can trigger these actions and passes the data to the service layer."

#### 2. StaffService
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/service/StaffService.java`
- **Class name:** `StaffService`
- **Purpose:** Core business logic for managing staff lifecycles, permissions, and email invitations.
- **Why we use it:** Centralizes all complex validation, security rules, and database operations.
- **Important methods/functions:**
  - `createStaff(CreateStaffRequest)`: Validates input, prevents duplicates, resolves branch access, creates User/Staff entities, hashes a temp password, sends the email, and logs the audit.
  - `resendInvite(Long)`: Generates a new temp password, hashes it, resends the email, and updates the user status.
  - `updateStaff(...)`: Validates email/phone conflicts, applies strict role upgrade/downgrade rules, and saves.
  - `activateStaff(Long)` / `deactivateStaff(Long)`: Toggles the `isActive` flag.
  - `resolveTargetBranch()`: Complex logic ensuring `ADMIN`s can only assign staff to their own branch, while `SUPER_ADMIN`s have global scope.
- **Input DTO:** `CreateStaffRequest`, `UpdateStaffRequest`.
- **Response DTO:** `StaffResponse`, `CreateStaffResponse`.
- **Step-by-step logic (Create):** Validates empty fields and duplicates. Checks if the creator is an ADMIN trying to create a role they aren't allowed to. Generates a 10-char password, encodes it. Saves `User`. Saves `Staff`. Calls `EmailService`. Returns success/failure response.
- **What other files it calls:** `UserRepository`, `StaffRepository`, `RoleRepository`, `BranchRepository`, `EmailService`, `PasswordEncoder`, `AuditLogService`.
- **Database tables/entities involved:** `User`, `Staff`, `Role`, `Branch`, `AuditLog`.
- **Validation rules:** Strict checks on email format, 10-digit phone number, branch scoping, and role restrictions.
- **Error cases:** Throws `RuntimeException` for validation failures (e.g., "Email already exists", "ADMIN can create staff only for their own branch").
- **Access rules:** Programmatic checks (`ensureCanManageTarget()`) ensure `ADMIN`s can only manage staff within their specific branch.
- **Code review explanation:** "This is the heaviest file. It handles all business logic. It ensures that standard Admins are physically restricted to managing staff within their own branch, automatically generates and hashes temporary passwords, and orchestrates the email invite flow."

#### 3. SmtpEmailService
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/service/email/SmtpEmailService.java`
- **Class name:** `SmtpEmailService` (implements `EmailService`)
- **Purpose:** Handles the physical transmission of emails using SMTP.
- **Why we use it:** To decouple email sending logic from the core `StaffService`.
- **Important methods/functions:**
  - `sendStaffInviteEmail(String toEmail, String username, String temporaryPassword)`
- **Input / Response:** Takes raw strings, returns `void`.
- **Step-by-step logic:** Validates the email string, fetches the subject and body from `EmailTemplateService`, constructs a `SimpleMailMessage`, and sends it via Spring's `JavaMailSender`.
- **What other files it calls:** `EmailTemplateService`, `JavaMailSender`.
- **Database tables/entities involved:** None.
- **Error cases:** Throws `IllegalArgumentException` on bad email format. Can throw Spring mail exceptions if SMTP fails.
- **Code review explanation:** "This handles the actual sending of emails over SMTP. It uses a template service to get the body content, ensuring our email transmission logic remains clean."

#### 4. EmailTemplateService
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/service/email/EmailTemplateService.java`
- **Class name:** `EmailTemplateService`
- **Purpose:** Stores the plain-text templates for outgoing emails.
- **Why we use it:** Keeps hardcoded strings out of the logic classes.
- **Important methods/functions:**
  - `buildStaffInviteEmailBody(username, password)`
- **Step-by-step logic:** Concatenates strings to form the email body containing the frontend login URL and plaintext temporary password.
- **Code review explanation:** "This class simply builds the string message that the user will read in their inbox."

#### 5. Password Hashing and Temporary Passwords
- **Why we hash the temp password:** If a database is compromised, plaintext passwords (even temporary ones) allow attackers immediate access to user accounts. Hashing ensures security from day zero.
- **Handling:** A random 10-character string is generated, hashed with BCrypt, and saved to the DB. The plaintext version is *only* passed directly to the `EmailService` to be mailed, and then discarded from memory. It is never saved.

---

### Code Review Criteria Check

#### 1. Code Formatting
- **Are the files properly indented and readable?** Yes, the codebase strictly adheres to standard Java spacing and formatting.
- **Are there formatting inconsistencies?** No major inconsistencies found.
- **Files needing cleanup:** None.

#### 2. Naming Conventions
- **Are names clear?** Yes. `CreateStaffRequest`, `SmtpEmailService`, `StaffController` are highly descriptive.
- **Confusing/Inconsistent names:** None. 

#### 3. Comments and Documentation
- **Are comments useful?** The code is largely self-documenting. `EmailTemplateService` contains a useful comment explaining that the `localhost` URL needs to be changed in production.
- **Unnecessary commented-out code?** None found.
- **Methods needing explanatory comments:** 
  - `StaffService.resolveTargetBranch()`: This method contains complex branch-resolution logic and could benefit from inline comments explaining the difference between SUPER_ADMIN and ADMIN workflows.

#### 4. No Hardcoding
- **Identified hardcoded values:**
  - `StaffService`: Hardcoded role names like `"SUPER_ADMIN"`, `"ADMIN"`, `"MANAGER"`. **Suggestion:** Move to an `Enum` (e.g., `RoleType.ADMIN.name()`).
  - `StaffService`: Error messages like `"Email already exists"`. **Acceptable**, but could be moved to a constants file.
  - `EmailTemplateService`: `loginUrl = "http://localhost:5173/staff/login"`. **Suggestion:** Move to `application.properties` (e.g., `${app.frontend.url}`).
  - `EmailTemplateService`: Hardcoded email subject and body. **Acceptable** for plain text, but could be moved to resources/templates if HTML emails are desired later.

#### 5. Separation of Concerns
- **Proper separation?** Excellent. 
  - `StaffController` handles HTTP.
  - `StaffService` handles the heavy business logic.
  - `SmtpEmailService` handles SMTP connections.
  - `EmailTemplateService` handles message formatting.
- **Logic mixed together?** `StaffService` is quite large (over 800 lines). While concerns are logically separated, the sheer size of `StaffService` suggests that some validation logic (like `isValidEmail` and `isValidPhone`) could be moved to a generic `ValidationUtils` class.

#### 6. Database/Data Type Understanding
- **Main fields used:** 
  - `User`: `fullName`, `email`, `phone`, `password`, `isActive`, `inviteStatus`, `emailSent`.
  - `Staff`: `employmentStatus`, relation to `Branch`.
- **Why suitable:** 
  - Enums (`InviteStatus`, `EmploymentStatus`) ensure data integrity over simple strings.
  - Booleans (`isActive`, `emailSent`) are optimal for flags.
- **Inefficient/Risky fields:** None.

#### 7. Testing Readiness
- **How to test in Postman:** 
  - **Create Staff:** POST to `/api/admin/staff` with Body `{"fullName": "John", "email": "j@test.com", "phone": "1234567890", "username": "john123", "roleName": "MANAGER", "branchId": 1}`.
  - **Resend Invite:** POST to `/api/admin/staff/{id}/resend-invite`.
  - **Update Staff:** PUT to `/api/admin/staff/{id}`.
  - **Activate/Deactivate:** PATCH to `/api/admin/staff/{id}/activate` or `/deactivate`.
  - **Get by Branch:** GET `/api/admin/staff/branch/{branchId}`.
- **Important test cases:**
  - *Success*: SUPER_ADMIN creates a new MANAGER successfully.
  - *Failure (Validation)*: Try creating a staff with an invalid 9-digit phone number.
  - *Failure (Access)*: Log in as a standard ADMIN and try to create staff for a different branch ID (should fail).

#### 8. Code Modification Readiness
- **Change 1: "Change the invite email message."**
  - *Modify:* `EmailTemplateService.java`. Update the string concatenation.
- **Change 2: "Change temporary password length to 12 characters."**
  - *Modify:* `StaffService.java` -> inside the `generateTempPassword()` method, change `int length = 10;` to `12`.
- **Change 3: "Prevent SUPER_ADMIN from assigning staff to an INACTIVE branch."**
  - *Modify:* `StaffService.java`. Currently, it checks for `BranchStatus.ACTIVE` in `resolveTargetBranch`, so this is actually already implemented!

#### 9. Error Handling and Validation
- **Existing validations:** Regex for email format, exact 10-digit check for phone, duplication checks against the `User` table for email, username, and phone.
- **Errors handled:** Fails gracefully via `RuntimeException` catching duplicate entries and unauthorized branch cross-overs. Handles SMTP failures gracefully by catching the exception and setting `emailSent = false` and `inviteStatus = FAILED` in the database, allowing for a retry later.
- **Missing validations:** DTOs (`CreateStaffRequest`) lack Spring `@Valid` annotations. Relying entirely on manual `if (isBlank(...))` checks inside the service is slightly verbose but functional.

#### 10. Review Explanation
"In Part 2, I implemented a robust Staff Management system with strict multi-tenant access controls. `SUPER_ADMIN`s have global access, while standard `ADMIN`s are programmatically locked to managing staff only within their assigned branch. When an account is created, we auto-generate a secure temporary password, immediately hash it via BCrypt for database security, and hand the plaintext version off to our decoupled `SmtpEmailService` to invite the user. If the SMTP server fails, the system catches the error, marks the invite status as 'FAILED' in the database, and allows admins to seamlessly trigger a 'resend invite' flow later."

---

### Summary

- **All staff-related endpoints found:**
  - `POST /api/admin/staff`
  - `POST /api/admin/staff/{id}/resend-invite`
  - `GET /api/admin/staff`
  - `GET /api/admin/staff/{id}`
  - `PUT /api/admin/staff/{id}`
  - `PATCH /api/admin/staff/{id}/activate`
  - `PATCH /api/admin/staff/{id}/deactivate`
  - `GET /api/admin/staff/branch/{branchId}`
  - `GET /api/admin/staff/role/{roleName}`
- **Files analyzed:** `StaffController`, `StaffService`, `SmtpEmailService`, `EmailTemplateService`.
- **How staff connects with User, Role, Branch, and Email:** A `Staff` entity belongs to a `Branch` and maps 1-to-1 to a generic `User` entity. The `User` entity holds the `Role` and authentication credentials. The `EmailService` acts as an external utility triggered upon `User` creation.
- **What I should explain during review:** Focus heavily on the **Branch-Level Isolation** (Admins can only see/edit their own branch) and the **Security** of hashing the temporary password instantly. 
- **Any missing or unclear files:** The system is comprehensive and well-structured. No missing components were identified.
