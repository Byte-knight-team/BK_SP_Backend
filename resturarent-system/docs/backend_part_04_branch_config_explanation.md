# Backend Part 04 - Branch Management and System Configuration Explanation

**Project:** QR & Online Restaurant Management System
**Member:** Member 01 - System Governance & Access Control
**Backend stack:** Spring Boot, Spring Security, JWT, JPA/Hibernate, MySQL

---

## PART 4: Branch Management and System Configuration

### Flow Diagrams

**Branch Create Flow:**
```text
BranchController
-> BranchService
-> Validate duplicate branch name
-> BranchRepository
-> Branch saved as ACTIVE
-> Response DTO
```

**Branch Update Flow:**
```text
BranchController
-> BranchService
-> Find branch
-> Validate duplicate fields (name)
-> Update branch fields
-> Save branch
-> Response DTO
```

**Branch Activate/Deactivate Flow:**
```text
BranchController
-> BranchService
-> Find branch
-> Change status ACTIVE/INACTIVE
-> Save branch
```

**Global Config Flow:**
```text
ConfigController
-> ConfigService
-> Load existing global config
-> Update allowed fields (taxes, loyalty)
-> Save config
-> Return response
```

**Branch Config Flow:**
```text
ConfigController
-> ConfigService
-> Find branch
-> Load or create branch config
-> Update branch-level settings (delivery fee, toggles)
-> Save config
-> Return response
```

**Effective Branch Config Flow:**
```text
Request branch effective config
-> Load global config
-> Load branch config
-> Load operating hours
-> Merge values
-> Return effective config response
```

---

### A. Branch Management Analysis

#### 1. Core Concepts
- **Branch Creation/Listing/Update:** Managed via `BranchController` and `BranchService`. Handled via `CreateBranchRequest` and `UpdateBranchRequest`.
- **Status Toggling:** Branches can be activated or deactivated via specific `PATCH` endpoints. The system uses an `ACTIVE`/`INACTIVE` Enum status rather than soft-deleting rows from the database. This prevents dangling foreign keys for orders or staff tied to that branch.
- **Connection with Staff:** The `Staff` entity contains a `@ManyToOne` relationship to `Branch`. A staff member belongs to exactly one branch.
- **Access Rules:** Branch creation, updating, and toggling are strictly locked to `SUPER_ADMIN`. Standard admins cannot create or deactivate branches.

#### 2. Detailed File Breakdown

**BranchController**
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/controller/BranchController.java`
- **Purpose:** API endpoints for managing the physical restaurant branches.
- **Why we use it:** To allow Super Admins to define new locations.
- **Important methods:** `createBranch`, `getAllBranches`, `getBranchById`, `updateBranch`, `activateBranch`, `deactivateBranch`.
- **Access rules:** All endpoints are protected with `@PreAuthorize("hasRole('SUPER_ADMIN')")`.

**BranchService**
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/service/BranchService.java`
- **Purpose:** Business logic and validation for branches.
- **Important methods:**
  - `createBranch(CreateBranchRequest request)`: Validates name, address, contact format, and email format. Checks for duplicate names in the DB. Saves the branch with an initial `ACTIVE` status.
  - `updateBranch(Long id, UpdateBranchRequest request)`: Fetches branch, validates new fields (including duplicate name check), saves, and audits.
  - `activateBranch` / `deactivateBranch`: Modifies the `BranchStatus` enum without deleting the row.
- **Database entities:** `Branch`.
- **Code review explanation:** "BranchService strictly ensures that branch names are unique and contact information is valid. We don't delete branches; we deactivate them. This preserves historical data for orders and staff tied to that branch."

**Branch Entity**
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/entity/Branch.java`
- **Fields:** `id`, `name`, `address`, `contactNumber`, `email`, `status`, `createdAt`.
- **Data Types:** Uses `String` for text, `BranchStatus` Enum for safety, and `LocalDateTime` for timestamps.

---

### B. System Configuration Analysis

#### 1. Core Concepts
- **Global Configuration:** Settings that apply to the whole system. Includes Tax Settings, Service Charges, Loyalty Points logic, and Order Cancellation windows.
- **Branch Configuration:** Settings specific to a single physical location. Includes Delivery Fee, Delivery Enabled, Pickup Enabled, Dine-In Enabled, and whether the branch is currently accepting orders.
- **Operating Hours:** A backend-supported feature defining open, close, and last-order times per day of the week.
- **Effective Configuration:** A unified DTO that merges Global Config, Branch Config, and Operating Hours into a single response, making it easy for frontend modules (like the Menu or Cart) to read everything at once.
- **Access Rules:** `SUPER_ADMIN` can modify global and all branch configs. `ADMIN` can *only* modify the config and operating hours for their *own* specific branch.

#### 2. Detailed File Breakdown

**SystemConfigController**
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/controller/SystemConfigController.java`
- **Purpose:** API endpoints for both global and branch-level settings.
- **Methods:** `getGlobalConfig`, `updateGlobalConfig`, `getBranchConfig`, `updateBranchConfig`, `getOperatingHours`, `updateOperatingHours`, `getEffectiveBranchConfig`.
- **Access:** `updateGlobalConfig` is `SUPER_ADMIN` only. Branch-level endpoints allow `hasAnyRole('SUPER_ADMIN', 'ADMIN')` but defer the actual security validation to the service layer.

**SystemConfigService**
- **Exact file path:** `src/main/java/com/ByteKnights/com/resturarent_system/service/SystemConfigService.java`
- **Purpose:** Business logic, default fallbacks, and multi-tenant security for config.
- **Important methods:**
  - `updateGlobalConfig(...)`: Updates taxes, loyalty rates. Validates percentages.
  - `updateBranchConfig(Long branchId, ...)`: Updates delivery fees and feature toggles.
  - `updateOperatingHours(...)`: Clears old hours for the branch, loops through the new week, validates `closeTime` is after `openTime`, and saves.
  - `validateAdminBranchWriteAccess(Long branchId)`: Core security method. Checks if the caller is an `ADMIN`. If so, fetches their `Staff` profile and throws an error if they try to edit a different branch's config.
  - `getEffectiveBranchConfig(Long branchId)`: Fetches global, branch, and hours, merging them into an `EffectiveBranchConfigResponse`.
- **Code review explanation:** "SystemConfigService is heavily fortified. While an Admin can access branch config APIs, the service intercepts the request, checks the Admin's staff profile, and enforces that they can only modify their own branch's delivery fees and hours. It also merges global and branch configs into a single 'Effective' payload for the frontend."

**Config Entities**
- `SystemConfig.java`: Contains `BigDecimal` for percentages (tax, service charge) and currency (amounts per point). `Integer` for minutes.
- `BranchConfig.java`: Contains `BigDecimal` for delivery fee. `boolean` for toggles (deliveryEnabled, dineInEnabled).
- `BranchOperatingHours.java`: Contains `DayOfWeek` enum, `LocalTime` for precise hours.

---

### Code Review Criteria Check

#### 1. Code Formatting
- **Readable and Indented?** Yes. `SystemConfigService` is large but well-structured with private helper methods.
- **Files needing cleanup:** None.

#### 2. Naming Conventions
- **Clear names?** Yes. DTOs like `EffectiveBranchConfigResponse` clearly describe their payload. Methods like `validateAdminBranchWriteAccess` perfectly describe their intent.
- **Inconsistent names:** None.

#### 3. Comments and Documentation
- **Useful comments?** The code relies on highly descriptive method names rather than inline comments.
- **Missing comments:** The `getEffectiveBranchConfig` method could use a comment explaining that it is designed to be the primary endpoint consumed by the Customer/Order modules.

#### 4. No Hardcoding
- **Identified hardcoded values:**
  - `SystemConfigService`: Default fallback values (e.g., `taxEnabled = false`, `taxPercentage = BigDecimal.ZERO`) are hardcoded in the entity defaults. **Acceptable**, as they are immediately editable via the DB/API.
  - Role strings (`"SUPER_ADMIN"`, `"ADMIN"`) are hardcoded in the service layer for validation. **Acceptable**, but could be moved to constants.

#### 5. Separation of Concerns
- **Proper separation?** Excellent. The controllers are incredibly thin. The `SystemConfigService` manages the complex merging and security validation logic, keeping it out of the controllers.

#### 6. Database/Data Type Understanding
- **Data Types Used:**
  - `BigDecimal`: Used for `taxPercentage`, `deliveryFee`, `valuePerPoint`. **Why?** Floating-point math (like `double`) can cause precision errors in financial calculations. `BigDecimal` is the absolute standard for money and exact percentages in Java.
  - `LocalTime`: Used for operating hours, separating time logic from dates.
  - `boolean`: Used for system toggles (e.g., `dineInEnabled`).
- **Risky fields:** None. The use of `BigDecimal` proves strong database and financial safety awareness.

#### 7. Testing Readiness
- **Testing in Postman:**
  - **Create Branch:** POST `/api/admin/branches` with `{"name": "Downtown", "contactNumber": "1234567890"}`.
  - **Update Global Config:** PUT `/api/admin/config/global` with `{"taxEnabled": true, "taxPercentage": 10.5}`.
  - **Get Effective Config:** GET `/api/admin/config/branches/1/effective`.
- **Important cases:**
  - *Success*: SUPER_ADMIN updates Global Config.
  - *Failure (Validation)*: Send `taxPercentage: 150` -> Fails ("must be between 0 and 100").
  - *Failure (Access)*: Log in as ADMIN for Branch 1, try to PUT config for Branch 2 -> Fails ("ADMIN can access configuration only for their own branch").

#### 8. Code Modification Readiness
- **Change 1: "Add a 'Packaging Fee' to the branch config."**
  - *Modify:* `BranchConfig.java` (add field), `UpdateBranchConfigRequest.java`, `BranchConfigResponse.java`, and update the mapping logic in `SystemConfigService.java`.
- **Change 2: "Prevent updating operating hours if the branch is INACTIVE."**
  - *Modify:* `SystemConfigService.java` -> inside `updateOperatingHours`, add a check: `if(branch.getStatus() == BranchStatus.INACTIVE) throw...`
- **Change 3: "Change the default delivery fee."**
  - *Modify:* `BranchConfig.java` entity default value from `BigDecimal.ZERO` to `new BigDecimal("5.00")`.

#### 9. Error Handling and Validation
- **Validations exist:** Branch names are checked for uniqueness. Tax/Service charges are capped between 0 and 100. Contact numbers use Regex `^[+0-9\\-\\s]{7,20}$`. Operating hours ensure `closeTime` is strictly after `openTime`.
- **Errors handled:** Throws `IllegalArgumentException` and `RuntimeException`, which are caught by the framework and return 500/400 codes.
- **Weaknesses:** Similar to previous modules, adding a `@ControllerAdvice` to format these exception strings into standard JSON error objects would be a great polish step.

#### 10. Review Explanation
"In Part 4, I handled the core physical infrastructure of the system. Branches are managed exclusively by Super Admins and use a soft-deactivate approach to preserve relational data. For configuration, I separated global financial rules (taxes, loyalty) from localized branch rules (delivery fees, operating hours). Crucially, I implemented a strict multi-tenant check in the `SystemConfigService` to guarantee that standard Admins can only modify the configuration of their assigned branch. Finally, I provided an 'Effective Config' endpoint that merges all these settings, providing a single, unified data payload for the Customer-facing menu modules to consume."

---

### Summary

- **All branch endpoints found:**
  - `POST /api/admin/branches`
  - `GET /api/admin/branches`
  - `GET /api/admin/branches/{id}`
  - `PUT /api/admin/branches/{id}`
  - `PATCH /api/admin/branches/{id}/activate`
  - `PATCH /api/admin/branches/{id}/deactivate`
- **All config endpoints found:**
  - `GET /api/admin/config/global`
  - `PUT /api/admin/config/global`
  - `GET /api/admin/config/branches/{branchId}`
  - `PUT /api/admin/config/branches/{branchId}`
  - `GET /api/admin/config/branches/{branchId}/operating-hours`
  - `PUT /api/admin/config/branches/{branchId}/operating-hours`
  - `GET /api/admin/config/branches/{branchId}/effective`
- **Files analyzed:** `BranchController`, `SystemConfigController`, `BranchService`, `SystemConfigService`, `Branch`, `SystemConfig`, `BranchConfig`, `BranchOperatingHours`.
- **How branch/config connects with other modules:** The `EffectiveBranchConfig` is the master rulebook. The Menu, Cart, and Order modules will read this configuration to know whether to apply taxes, whether delivery is currently enabled, and if the branch is currently open. Staff are directly tied to Branches.
- **What I should explain during review:** Highlight the use of `BigDecimal` for financial accuracy. Emphasize the logic in `validateAdminBranchWriteAccess` which restricts Admins to their own branch.
- **Any missing or unclear files:** Payment configuration was explicitly excluded from this analysis as requested, but the current configuration ecosystem is robust and production-ready.
