# Backend Part 03 - RBAC Roles Privileges and Permissions Explanation

**Project:** QR & Online Restaurant Management System
**Member:** Member 01 - System Governance & Access Control
**Backend stack:** Spring Boot, Spring Security, JWT, JPA/Hibernate, MySQL

---

## PART 3: RBAC - Roles, Privileges, and Permission Assignment

### Flow Diagrams

**RBAC Permission Save Flow:**
```text
Frontend Roles page
-> PUT role permissions endpoint
-> RoleController
-> RoleService
-> RoleRepository
-> PrivilegeRepository
-> role_permission table
-> Updated permissions returned
```

**Privilege List Flow:**
```text
Frontend Roles page
-> GET privileges endpoint
-> PrivilegeController
-> PrivilegeRepository
-> Privilege list returned
```

**Endpoint Access Flow:**
```text
Request with JWT
-> Security filter validates token
-> User role loaded
-> Spring Security / PreAuthorize checks role
-> Request allowed or denied
```

**Role Delete Flow:**
```text
Request delete role
-> Check role exists
-> Check whether role is core role
-> Check whether role is assigned to users
-> Delete only if safe
```

---

### Detailed Analysis of Relationships and Concepts

**1. Role Entity/Model**
- A `Role` represents a job function or title (e.g., SUPER_ADMIN, MANAGER, CHEF). It is used to quickly grant large sets of access rights.

**2. Privilege/Permission Entity/Model**
- A `Privilege` (or permission) represents a highly granular action (e.g., "VIEW_REPORTS", "EDIT_MENU").

**3. Role-Permission Relationship**
- **Type:** Many-to-Many (`@ManyToMany`).
- **Implementation:** A `Role` contains a `Set<Privilege>`. In the database, this creates a join table called `role_permissions` mapping `role_id` to `permission_id`.

**4. User-Role Relationship**
- **Type:** Many-to-One (`@ManyToOne`).
- **Implementation:** Multiple `User` entities can share one `Role`. The `users` table has a `role_id` foreign key.

**13. Core Role Protection & 18. Editable/Protected Roles**
- Handled in `RoleService.java` using a static `CORE_ROLES` Set containing `"SUPER_ADMIN", "ADMIN", "MANAGER", "CHEF", "RECEPTIONIST", "DELIVERY", "CUSTOMER"`.
- Core roles **cannot** be renamed or deleted to prevent accidental system breakage. However, their permissions can be updated. Custom roles created later are fully editable and deletable.

**14. How RBAC Protects Endpoints & 16/17. Role Names and Spring Security**
- Endpoints are protected using annotations like `@PreAuthorize("hasRole('SUPER_ADMIN')")`.
- When `JwtAuthenticationFilter` authenticates a user, it creates a `JwtUserPrincipal`. Inside this principal, Spring's `SimpleGrantedAuthority` is explicitly assigned the role name prefixed with `ROLE_` (e.g., `ROLE_SUPER_ADMIN`).
- When `@PreAuthorize("hasRole('SUPER_ADMIN')")` is called, Spring automatically prepends `ROLE_` to "SUPER_ADMIN" and checks if it exists in the principal's authorities.

**15. Difference Between Role Checks and Privilege Checks**
- **Currently Implemented:** The backend endpoints exclusively use **Role Checks** (e.g., `hasRole('ADMIN')`).
- **Future/UI Use:** While privileges are stored in the database and assigned to roles, they are not currently loaded into Spring Security's authority context. The privilege assignments are primarily sent to the frontend to drive UI component visibility, allowing granular UI hiding without writing complex backend rules yet.

---

### Detailed File Analysis

#### 1. Role (Entity)
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/entity/Role.java`
- **Class name:** `Role`
- **Purpose:** Database entity representing user roles.
- **Why we use it:** To map the `roles` table and establish the ManyToMany relationship with `Privileges`.

#### 2. Privilege (Entity)
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/entity/Privilege.java`
- **Class name:** `Privilege`
- **Purpose:** Database entity representing granular permissions.
- **Why we use it:** To map the `privileges` table and store individual atomic rights.

#### 3. RoleController
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/controller/RoleController.java`
- **Class name:** `RoleController`
- **Purpose:** API endpoints for managing Roles and their permissions.
- **Why we use it:** Allows the Super Admin to view, create, edit, and delete roles, and assign privileges.
- **Important methods/functions:**
  - `getAllRoles`, `getRoleSummary`, `getPermissions`, `createRole`, `assignPermissions`, `updateRole`, `deleteRole`.
- **Request DTOs:** `UpdateRoleRequest`, `Map<String, String>` (for creation), `Set<String>` (for permission assignment).
- **Response DTOs:** `RoleSummaryResponse`, `Role`.
- **Access rules:** `GET` operations are allowed for `SUPER_ADMIN` and `ADMIN`. All creation/modification (`POST`, `PUT`, `DELETE`) is strictly locked to `SUPER_ADMIN`.
- **Code review explanation:** "This controller provides full CRUD capabilities for role management. It strictly isolates mutating actions to the Super Admin while allowing regular Admins to read the roles for assignment purposes."

#### 4. PrivilegeController
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/controller/PrivilegeController.java`
- **Class name:** `PrivilegeController`
- **Purpose:** Exposes a read-only endpoint for all available privileges.
- **Why we use it:** So the frontend Role Management UI can fetch the master list of all permissions to show checkboxes to the admin.
- **Important methods/functions:** `getAllPrivileges`.
- **Access rules:** Strictly `SUPER_ADMIN`.

#### 5. RoleService
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/service/RoleService.java`
- **Class name:** `RoleService`
- **Purpose:** Contains the business logic, validation, and protection rules for role manipulation.
- **Important Methods Explanation:**
  - **`assignPermissionsToRole`**
    - **Purpose:** Replaces all permissions for a specific role.
    - **Logic:** Fetches the role. Clears old permissions. Loops through the provided Set of permission names, fetches the `Privilege` entities from the DB, attaches them to the role, and saves.
    - **Protection:** Prevents a standard admin from modifying the `SUPER_ADMIN` role permissions if they somehow bypass the controller logic.
  - **`deleteRole`**
    - **Purpose:** Safely removes a role.
    - **Logic:** Fetches role. Checks if it is in the `CORE_ROLES` set (throws error if true). Checks if any `User` currently holds this role (throws error if true). Deletes if safe.
- **Error cases:** "Role not found", "Core roles cannot be renamed", "Core roles cannot be deleted", "Cannot delete role because it is assigned to users".
- **Code review explanation:** "RoleService holds our safety net. It prevents core system roles from being corrupted or deleted, and ensures we never delete a custom role that still has active staff assigned to it."

#### 6. JwtUserPrincipal
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/security/JwtUserPrincipal.java`
- **Class name:** `JwtUserPrincipal`
- **Purpose:** Bridges our custom `User` entity to Spring Security's context.
- **Important logic:** In the `getAuthorities()` method, it takes the user's role name, prepends `"ROLE_"`, and returns it as a `SimpleGrantedAuthority`. This is the exact mechanism that makes `@PreAuthorize("hasRole(...)")` work.

---

### Code Review Criteria Check

#### 1. Code Formatting
- **Are the files properly indented and readable?** Yes, classes are neat, spaced correctly, and readable.
- **Are there formatting inconsistencies?** No.
- **Files needing cleanup:** None.

#### 2. Naming Conventions
- **Are names clear?** Yes. `PrivilegeController`, `RoleSummaryResponse`, and `assignPermissionsToRole` are standard and descriptive.
- **Confusing/Inconsistent names:** None.

#### 3. Comments and Documentation
- **Are comments useful?** Minimal comments exist, but code is mostly self-documenting.
- **Unnecessary commented-out code?** None found.
- **Methods needing explanatory comments:** The `assignPermissionsToRole` method could use an inline comment explaining that it *replaces* rather than *appends* permissions.

#### 4. No Hardcoding
- **Identified hardcoded values:**
  - `RoleService`: `CORE_ROLES` Set contains hardcoded strings (`"SUPER_ADMIN"`, `"ADMIN"`, etc.). **Acceptable** because core architectural roles rarely change, though an `Enum` would be slightly safer for refactoring.
  - `RoleService`: Error messages. **Acceptable**, but could be localized.
  - `JwtUserPrincipal`: `"ROLE_"` string prefix. **Required** by Spring Security conventions.

#### 5. Separation of Concerns
- **Proper separation?** Yes. Controllers handle HTTP, `RoleService` handles safety checks and DB calls, Entities map the DB, and DTOs transport summary data without leaking massive object graphs.

#### 6. Database/Data Type Understanding
- **Relationships:**
  - `roles` (1) <-to-> (Many) `users` (A user has one role, a role has many users).
  - `roles` (Many) <-to-> (Many) `privileges` (Handled via `role_permissions` join table).
- **Why suitable:** Using a `@ManyToMany` join table for permissions is the industry standard for RBAC, allowing infinite, many-directional assignments without database schema changes.

#### 7. Testing Readiness
- **How to test in Postman:** 
  - **Create Role:** POST `/api/admin/roles` with Body `{"name": "WAITER", "description": "Handles tables"}`.
  - **Assign Permissions:** PUT `/api/admin/roles/{roleId}/permissions` with JSON Array `["VIEW_MENU", "CREATE_ORDER"]`.
  - **Delete Role:** DELETE `/api/admin/roles/{roleId}`.
- **Important test cases:**
  - *Success*: Creating a new role.
  - *Failure (Protection)*: Try to DELETE the role with ID 1 (SUPER_ADMIN) -> Should return 500/400 "Core roles cannot be deleted".
  - *Failure (Constraint)*: Try to DELETE a custom role that is currently assigned to a user -> Should return "Cannot delete... assigned to one or more users".

#### 8. Code Modification Readiness
- **Change 1: "Add a new core role called 'AUDITOR' that cannot be deleted."**
  - *Modify:* `RoleService.java` -> Add `"AUDITOR"` to the `CORE_ROLES` Set.
- **Change 2: "Make Spring Security check granular privileges instead of broad Roles."**
  - *Modify:* `JwtUserPrincipal.java` -> Iterate over `user.getRole().getPermissions()` and add each `Privilege.getName()` to the `GrantedAuthority` list. Then update controllers to use `@PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")`.
- **Change 3: "Add a new endpoint to remove a single permission from a role."**
  - *Modify:* `RoleController.java` (add a `DELETE` endpoint), and `RoleService.java` (fetch role, `.getPermissions().remove(privilege)`, save).

#### 9. Error Handling and Validation
- **Existing validations:** Prevents duplicate role names on creation and update. Prevents core roles from being altered or deleted. Prevents deletion of active roles.
- **Errors handled:** Throws specific `RuntimeException`s which stop execution and rollback the database transaction gracefully.
- **Missing validations:** Similar to Part 2, standard `@Valid` and `@ControllerAdvice` global exception handling would clean up the JSON responses significantly.

#### 10. Review Explanation
"In Part 3, the RBAC foundation is built on standard Spring Data JPA relationships. We use a `@ManyToOne` mapping for User-to-Role and a `@ManyToMany` join table for Role-to-Privilege. The most critical piece of logic resides in the `RoleService`, which strictly protects our 'Core Roles' from deletion or renaming, preventing system lockouts. Additionally, by prefixing the role name with 'ROLE_' inside the `JwtUserPrincipal`, we seamlessly integrate our custom database tables directly into Spring Security's `@PreAuthorize` annotation ecosystem."

---

### Summary

- **All RBAC endpoints found:**
  - `GET /api/admin/roles`
  - `GET /api/admin/roles/{roleId}`
  - `GET /api/admin/roles/{roleId}/permissions`
  - `POST /api/admin/roles`
  - `PUT /api/admin/roles/{roleId}/permissions`
  - `PUT /api/admin/roles/{roleId}`
  - `DELETE /api/admin/roles/{roleId}`
  - `GET /api/admin/privileges`
- **All role/privilege files analyzed:** `Role`, `Privilege`, `User` (entities), `RoleController`, `PrivilegeController`, `RoleService`, `JwtUserPrincipal`.
- **Database relationship explanation:** `users` table holds a `role_id`. `roles` table links to `privileges` table via a `role_permissions` join table mapping `role_id` to `permission_id`.
- **What I should explain during review:** Highlight how `CORE_ROLES` are protected in the service layer, and clarify that currently, the backend `@PreAuthorize` utilizes broad Role checks, while the granular Privileges are designed to be sent to the frontend for dynamic UI rendering.
- **Any missing or unclear files:** Everything is clear and standard. Implementing global exception handlers in the future is the only notable improvement.
