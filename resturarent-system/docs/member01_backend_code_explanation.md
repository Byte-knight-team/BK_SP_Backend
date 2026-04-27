# Member 01 Backend Code Explanation - QR & Online Restaurant Management System

## PART 1: Authentication, JWT, and Spring Security

### Flow Diagrams

**Staff Login Flow:**
```text
Frontend login request
-> AuthController
-> AuthService
-> UserRepository
-> PasswordEncoder
-> JwtService
-> JWT returned to frontend
```

**Change Password Flow:**
```text
Authenticated staff
-> Change password endpoint
-> Validate current password
-> Encode new password
-> Update passwordChanged status
-> Save user
```

---

### Detailed Analysis of Files

#### 1. AuthController
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/controller/AuthController.java`
- **Class name:** `AuthController`
- **Purpose:** Exposes HTTP endpoints for authentication.
- **Why we use this file:** It is the entry point for the frontend to communicate with the backend regarding login and password change features.
- **Important methods/functions:** 
  - `staffLogin(StaffLoginRequest request)`
  - `changePassword(ChangePasswordRequest request)`
- **Step-by-step logic:** Receives a request payload, passes the data to `AuthService`, and returns the appropriate HTTP response back to the client.
- **What other files it connects to:** `AuthService`, `StaffLoginRequest`, `ChangePasswordRequest`, `LoginResponse`.
- **What database table/entity it uses:** Indirectly interacts with User and Staff entities through the Service layer.
- **Possible errors:** Standard validation errors (e.g., bad request 400).
- **Simple explanation I can say in code review:** "This is our API controller. It receives login and change-password requests from the frontend and sends them to the service layer for processing."

#### 2. AuthService
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/auth/AuthService.java`
- **Class name:** `AuthService`
- **Purpose:** Contains the core business logic for user authentication.
- **Why we use this file:** To separate business logic from the controller, keeping our code clean and manageable.
- **Important methods/functions:** 
  - `loginStaff(StaffLoginRequest request)`
  - `changePassword(ChangePasswordRequest request)`
- **Step-by-step logic:** 
  - For login: Checks if the user exists via email. Checks if the account is active. Verifies the password using `PasswordEncoder`. Generates a JWT using `JwtService`. Also logs the success or failure using `AuditLogService`. 
  - For change password: Validates current password, encodes the new one, updates the `passwordChanged` flag, and saves to the DB.
- **What other files it connects to:** `UserRepository`, `StaffRepository`, `PasswordEncoder`, `JwtService`, `AuditLogService`.
- **What database table/entity it uses:** `User`, `Staff`, and audit log tables.
- **Possible errors:** "Invalid email", "Account disabled", "Invalid password", "Current password is incorrect".
- **Simple explanation I can say in code review:** "This is the brain of the authentication. It checks if the email and password are correct, ensures the user is active, logs the login attempt, and finally creates a JWT for them."

#### 3. JwtService
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/security/JwtService.java`
- **Class name:** `JwtService`
- **Purpose:** Handles the creation and validation of JSON Web Tokens (JWT).
- **Why we use this file:** To manage stateless authentication security securely by signing and verifying tokens.
- **Important methods/functions:** 
  - `generateToken(Long userId, String email, String role)`
  - `validateToken(String token)`
  - `getUserIdFromToken(String token)`
- **Step-by-step logic:** 
  - Generation: Takes user ID, email, and role, sets an expiration date, and signs it using a secret key.
  - Validation: Parses the token using the secret key to ensure it hasn't been tampered with and checks if it's expired.
- **What other files it connects to:** Doesn't connect to many project files; mostly uses the `io.jsonwebtoken` library.
- **What database table/entity it uses:** None directly.
- **Possible errors:** Signature mismatch, expired token, malformed JWT.
- **Simple explanation I can say in code review:** "This file generates the secure 'key' (JWT) that we give to the frontend upon successful login, and verifies it on subsequent requests to ensure it's valid."

#### 4. SecurityConfig
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/config/SecurityConfig.java`
- **Class name:** `SecurityConfig`
- **Purpose:** The central security configuration for the application.
- **Why we use this file:** To tell Spring Security which endpoints are public, which are protected, and what roles are required for specific actions.
- **Important methods/functions:** 
  - `securityFilterChain(HttpSecurity http)`
- **Step-by-step logic:** Disables CSRF (common for REST APIs), sets session management to STATELESS, defines public endpoints (like login and swagger), sets up role-based access for `/api/admin/**` endpoints, and injects our custom JWT filters.
- **What other files it connects to:** `JwtAuthenticationFilter`, `ForcePasswordChangeFilter`.
- **What database table/entity it uses:** None directly.
- **Possible errors:** Misconfigured route patterns leading to unintended access (403 or 401).
- **Simple explanation I can say in code review:** "This file configures our global security rules. It acts as the bouncer, deciding which routes require authentication and which roles are allowed to access certain URLs."

#### 5. JwtAuthenticationFilter
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/security/JwtAuthenticationFilter.java`
- **Class name:** `JwtAuthenticationFilter`
- **Purpose:** Intercepts every incoming HTTP request to check for a JWT.
- **Why we use this file:** To ensure that requests to protected routes have a valid token identifying the user.
- **Important methods/functions:** 
  - `doFilterInternal(...)`
  - `getJwtFromRequest(...)`
- **Step-by-step logic:** Extracts the "Bearer " token from the Authorization header. If valid, it gets the user ID, loads the active user from the database, and tells Spring Security's `SecurityContextHolder` that this user is authenticated.
- **What other files it connects to:** `JwtService`, `UserRepository`, `JwtUserPrincipal`.
- **What database table/entity it uses:** `User` entity to verify if the account is still active.
- **Possible errors:** Throws exceptions if the user is not found or token parsing fails (handled gracefully).
- **Simple explanation I can say in code review:** "This is a filter that runs before the request hits our controllers. It checks the token in the header, figures out who is making the request, and logs them into the Spring Security context."

#### 6. ForcePasswordChangeFilter
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/security/ForcePasswordChangeFilter.java`
- **Class name:** `ForcePasswordChangeFilter`
- **Purpose:** Forces regular staff to change their default password.
- **Why we use this file:** To improve security by ensuring new staff members don't continue using temporary passwords assigned by admins.
- **Important methods/functions:** 
  - `doFilterInternal(...)`
- **Step-by-step logic:** Checks if the requested endpoint is login or change-password (allows them). Otherwise, checks the authenticated user's `passwordChanged` flag in the database. If it's `false` (and the user is not an admin), it blocks the request with a 403 Forbidden status.
- **What other files it connects to:** `UserRepository`, `User` entity.
- **What database table/entity it uses:** `User` entity.
- **Possible errors:** Sending a 403 Forbidden with a specific message.
- **Simple explanation I can say in code review:** "This filter blocks regular staff members from using any system features until they change their temporary password."

#### 7. SecurityBeansConfig (PasswordEncoder)
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/auth/SecurityBeansConfig.java`
- **Class name:** `SecurityBeansConfig`
- **Purpose:** Provides Spring Security Beans, particularly the `PasswordEncoder`.
- **Why we use this file:** To provide a BCrypt hasher to securely store and verify passwords without keeping them in plain text.
- **Important methods/functions:** 
  - `passwordEncoder()`
- **Step-by-step logic:** Initializes and returns a `BCryptPasswordEncoder` instance.
- **What other files it connects to:** Used by `AuthService`.
- **What database table/entity it uses:** None directly.
- **Possible errors:** None typically.
- **Simple explanation I can say in code review:** "This file simply sets up BCrypt so we can hash passwords securely before saving them to the database."

#### 8. LoginResponse
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/dto/LoginResponse.java`
- **Class name:** `LoginResponse`
- **Purpose:** A Data Transfer Object (DTO) defining the response sent to the frontend after a successful login.
- **Why we use this file:** To format exactly what data the frontend receives, keeping it structured.
- **Important methods/functions:** standard getters/setters via Lombok.
- **Step-by-step logic:** Carries the user ID, email, role name, active status, `passwordChanged` status, branch ID, branch name, the generated JWT token, and token type.
- **What other files it connects to:** Used heavily in `AuthService` and `AuthController`.
- **What database table/entity it uses:** None directly.
- **Possible errors:** None.
- **Simple explanation I can say in code review:** "This class defines the exact JSON payload the frontend gets when a user successfully logs in, including the token and their branch info."

#### 9. AuthenticationEntryPoint / AccessDeniedHandler
- **Analysis:** These components are **not explicitly implemented or configured** in the `SecurityConfig`.
- **Reason:** The system relies on Spring Boot's default handlers. If an unauthenticated user accesses a protected route, Spring sends a standard 403 Forbidden. The only custom denial logic is in the `ForcePasswordChangeFilter`.

#### 10. Protected Backend Routes Overview
- Defined in `SecurityConfig`.
- **Public:** Login, Swagger UI.
- **Requires SUPER_ADMIN:** Branch management, Role management, Privilege endpoints, Email testing.
- **Requires SUPER_ADMIN or ADMIN:** Staff management.
- **Requires Authentication:** Everything else (like changing passwords or regular staff endpoints).

---

### Summary for Project Review

**Key files analyzed:**
- `AuthController.java` (Endpoints)
- `AuthService.java` (Login/Password Logic)
- `JwtService.java` (Token Generation/Validation)
- `SecurityConfig.java` (Route Protection)
- `JwtAuthenticationFilter.java` (Token Verification on requests)
- `ForcePasswordChangeFilter.java` (Enforces password changes)

**Key endpoints found:**
- `POST /api/auth/staff/login` (Public)
- `PUT /api/auth/staff/change-password` (Authenticated)

**What I should explain during review:**
- "Our system uses stateless JWT authentication. When a user logs in, `AuthService` verifies their credentials and uses `BCrypt` to check the password. If successful, `JwtService` generates a token containing their role and ID."
- "For every subsequent request, the `JwtAuthenticationFilter` intercepts it, extracts the token, validates it, and logs the user into the Spring Security context."
- "We also have a custom `ForcePasswordChangeFilter` which checks if a newly created staff member has changed their temporary password. If not, they are blocked from accessing other routes until they do so."
- "Routing security is tightly controlled in `SecurityConfig`, ensuring that only SUPER_ADMIN or ADMIN roles can access sensitive branch and staff management endpoints."

**Any missing or unclear files:**
- `AuthenticationEntryPoint` and `AccessDeniedHandler` are missing from the explicit configuration. The system falls back on Spring's defaults. This is entirely acceptable for a REST API, but custom handlers could be added in the future if a specific JSON error structure is needed for 401/403 errors.

---

### Code Review Criteria Check

#### 1. Code Formatting
- **Are the files properly indented and readable?** Yes, the code follows standard Java indentation and is highly readable.
- **Are there any formatting inconsistencies?** No major inconsistencies. The structure within methods is consistent.
- **Files needing cleanup:** None specifically for formatting.

#### 2. Naming Conventions
- **Are names clear?** Yes. `AuthController`, `AuthService`, `JwtService`, `StaffLoginRequest`, and `LoginResponse` clearly describe their purposes.
- **Confusing/Inconsistent names:** None found. The naming convention perfectly aligns with standard Spring Boot architectural patterns.

#### 3. Comments and Documentation
- **Are comments useful?** The codebase currently lacks JavaDoc or inline comments. While the code is self-explanatory, a few comments would help.
- **Unnecessary commented-out code?** None found in these core files.
- **Methods needing explanatory comments:** 
  - `AuthService.changePassword()`: A brief comment explaining the flow (fetching user from context, verifying old password, updating flag) would be helpful for junior devs.
  - `ForcePasswordChangeFilter.doFilterInternal()`: A comment explaining *why* certain routes (like login) bypass the filter.

#### 4. No Hardcoding
- **Identified hardcoded values:**
  - `AuthController.java`: `@CrossOrigin(origins = "http://localhost:5173")` - **Should be moved** to a global `CorsConfig` or `application.properties` to easily switch between dev and production URLs.
  - `SecurityConfig.java`: Role names `"SUPER_ADMIN"`, `"ADMIN"` are hardcoded strings. - **Acceptable for MVP**, but better moved to an `Enum` (e.g., `RoleType.SUPER_ADMIN.name()`).
  - `AuthService.java`: Error messages like `"Invalid email"` or `"Account disabled"`. - **Acceptable**, but could be moved to a constants file for easier localization in the future.
  - `ForcePasswordChangeFilter.java`: Hardcoded JSON response string `{"success": false, "message": "..."}`. - **Acceptable** for a filter, but using `ObjectMapper` would be cleaner.
  - `JwtService.java`: JWT settings are correctly externalized using `@Value` (e.g., `${app.jwt.secret}`).

#### 5. Separation of Concerns
- **Proper separation?** Excellent. 
  - `AuthController` handles only HTTP requests and responses.
  - `AuthService` handles the business logic (checking passwords, updating DB).
  - `JwtService` purely handles token math.
  - Filters handle pre-request security checks.
- **Logic mixed together?** No. The separation is textbook Spring Boot architecture.

#### 6. Database/Data Type Understanding
- **Main fields used:** `email` (String), `password` (String), `isActive` (Boolean), `passwordChanged` (Boolean).
- **Why suitable:** 
  - `String` for passwords allows storing long BCrypt hashes.
  - `Boolean` is perfect for binary status flags like `isActive`.
- **Inefficient/Risky fields:** None. The types are standard and optimized.

#### 7. Testing Readiness
- **How to test in Postman:** 
  - Create a POST request to `http://localhost:8080/api/auth/staff/login`.
  - Add JSON body: `{"email": "staff@example.com", "password": "password123"}`.
- **Important test cases:**
  - *Success*: Valid email and password returns 200 OK with JWT token.
  - *Failure 1*: Invalid password returns 500/400.
  - *Failure 2*: Inactive user account returns error.
- **Mock data needed:** A `User` object with a pre-hashed BCrypt password inserted into the database via a Seeder.

#### 8. Code Modification Readiness
- **Change 1: "Add regex validation for strong passwords."**
  - *Modify:* `AuthService.changePassword()` to check the new password against a regex pattern before saving.
- **Change 2: "Change the JWT expiration time to 24 hours."**
  - *Modify:* `application.properties` (change `app.jwt.expiration-ms`). No Java code changes needed!
- **Change 3: "Add a 'last_login_date' tracker."**
  - *Modify:* `User` entity (add field), and `AuthService.loginStaff()` (update and save the field upon successful login).

#### 9. Error Handling and Validation
- **Existing validations:** Checks if the user exists, if the account is active, and if the password matches using `PasswordEncoder`.
- **Errors handled:** Incorrect credentials or disabled accounts are caught and throw `RuntimeException`.
- **Missing validations:** The `StaffLoginRequest` and `ChangePasswordRequest` DTOs lack `@NotBlank` or `@Email` annotations. Currently, empty passwords or bad emails fail deeper in the service rather than at the controller level. Also, throwing a generic `RuntimeException` results in a raw 500 error; a `@ControllerAdvice` global exception handler would be better.

#### 10. Review Explanation
"During my review of the authentication and security configuration, I found the code follows excellent separation of concerns. Controllers, services, and security filters are well isolated. However, to elevate the code quality for production, we should move hardcoded URLs like the CORS origin into our properties file, add `@Valid` annotations to our DTOs to catch bad inputs early, and implement a global exception handler to return clean JSON error messages instead of raw 500 errors."
