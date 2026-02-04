# Tampa Volunteers Authorization System Design

## Overview

This document describes the authorization system for Tampa Volunteers, including role-based access control, resource ownership, and command-line administration tools.

### Goals

1. **Super Admin Management** - CLI tools to manage platform-level administrators
2. **Organization Management** - Volunteer organizations can manage their opportunities and volunteers
3. **Volunteer Self-Service** - Volunteers can manage their registrations and hours
4. **User Profile Control** - Users can manage their profiles, visibility, and status
5. **Audit Visibility** - Super admins can view all users who have signed in

---

## Role Model

### User Roles (Existing + New)

| Role | Description |
|------|-------------|
| `VOLUNTEER` | Default role for new users. Can browse opportunities and register. |
| `ORG_ADMIN` | Can manage their organization(s) and associated opportunities. |
| `ADMIN` | Platform administrator with full read access. |
| `SUPER_ADMIN` | **New** - Full platform access including user management and system configuration. |

### Role Hierarchy

```
SUPER_ADMIN
    └── ADMIN
        └── ORG_ADMIN
            └── VOLUNTEER
```

Higher roles inherit all permissions of lower roles.

---

## Database Schema Changes

### 1. Modify `users` Table

Add new columns for profile visibility and user status:

```sql
-- V6__Add_User_Profile_Fields.sql

ALTER TABLE users ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN user_status VARCHAR(50) NOT NULL DEFAULT 'VOLUNTEER';
ALTER TABLE users ADD COLUMN bio TEXT;
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500);
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;

-- Update role enum to include SUPER_ADMIN
-- Note: PostgreSQL doesn't have ALTER ENUM, so role is stored as VARCHAR
-- Application code will validate the enum values

CREATE INDEX idx_users_is_public ON users(is_public);
CREATE INDEX idx_users_user_status ON users(user_status);
CREATE INDEX idx_users_last_login_at ON users(last_login_at);
```

### 2. Create `organization_members` Table

Support multiple admins per organization:

```sql
-- V7__Add_Organization_Members.sql

CREATE TABLE organization_members (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',  -- OWNER, ADMIN, MEMBER
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invited_by BIGINT REFERENCES users(id),
    CONSTRAINT uk_org_member UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_org_members_org ON organization_members(organization_id);
CREATE INDEX idx_org_members_user ON organization_members(user_id);
CREATE INDEX idx_org_members_role ON organization_members(role);
```

### 3. Create `admin_audit_log` Table

Track administrative actions:

```sql
-- V8__Add_Admin_Audit_Log.sql

CREATE TABLE admin_audit_log (
    id BIGSERIAL PRIMARY KEY,
    admin_user_id BIGINT NOT NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,  -- USER, ORGANIZATION, OPPORTUNITY
    target_id BIGINT,
    details JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_admin ON admin_audit_log(admin_user_id);
CREATE INDEX idx_audit_action ON admin_audit_log(action);
CREATE INDEX idx_audit_target ON admin_audit_log(target_type, target_id);
CREATE INDEX idx_audit_created ON admin_audit_log(created_at);
```

---

## User Status Model

Users can indicate their status on the platform:

```java
public enum UserStatus {
    VOLUNTEER,           // Looking to volunteer (default)
    ORG_REPRESENTATIVE,  // Represents an organization
    BOTH,                // Both volunteer and org representative
    INACTIVE             // Not actively participating
}
```

This is separate from `UserRole` and represents the user's self-declared participation mode.

---

## Permission Matrix

### Resource Permissions

| Resource | Action | VOLUNTEER | ORG_ADMIN | ADMIN | SUPER_ADMIN |
|----------|--------|-----------|-----------|-------|-------------|
| **Users** | View own profile | ✓ | ✓ | ✓ | ✓ |
| | Edit own profile | ✓ | ✓ | ✓ | ✓ |
| | View public profiles | ✓ | ✓ | ✓ | ✓ |
| | View all users | | | ✓ | ✓ |
| | Edit any user | | | | ✓ |
| | Delete any user | | | | ✓ |
| | Grant/revoke roles | | | | ✓ |
| **Organizations** | View public | ✓ | ✓ | ✓ | ✓ |
| | Create | ✓ | ✓ | ✓ | ✓ |
| | Edit own | | ✓* | ✓ | ✓ |
| | Delete own | | ✓* | | ✓ |
| | Verify | | | ✓ | ✓ |
| | Manage members | | ✓* | | ✓ |
| **Opportunities** | View published | ✓ | ✓ | ✓ | ✓ |
| | Create (for own org) | | ✓* | ✓ | ✓ |
| | Edit (for own org) | | ✓* | ✓ | ✓ |
| | Delete (for own org) | | ✓* | | ✓ |
| | View all drafts | | | ✓ | ✓ |
| **Registrations** | View own | ✓ | ✓ | ✓ | ✓ |
| | Create own | ✓ | ✓ | ✓ | ✓ |
| | Cancel own | ✓ | ✓ | ✓ | ✓ |
| | View for own org | | ✓* | ✓ | ✓ |
| | Manage for own org | | ✓* | ✓ | ✓ |
| **Volunteer Hours** | View own | ✓ | ✓ | ✓ | ✓ |
| | Log own | ✓ | ✓ | ✓ | ✓ |
| | Verify (for own org) | | ✓* | ✓ | ✓ |

*✓\* = Only for organizations where user is OWNER or ADMIN member*

---

## API Authorization

### Endpoint Security Configuration

Update `SecurityConfig.java` to enforce role-based access:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/auth/**", "/oauth2/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/opportunities", "/opportunities/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/organizations", "/organizations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/categories", "/skills").permitAll()
                .requestMatchers(HttpMethod.GET, "/users/public/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Admin endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/super-admin/**").hasRole("SUPER_ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // ... rest of config
    }
}
```

### Method-Level Security

Use `@PreAuthorize` annotations on service methods:

```java
@Service
public class OrganizationService {

    @PreAuthorize("hasRole('SUPER_ADMIN') or @authz.isOrgMember(#orgId, 'ADMIN')")
    public Organization updateOrganization(Long orgId, OrganizationDTO dto) {
        // ...
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or @authz.isOrgMember(#orgId, 'OWNER')")
    public void deleteOrganization(Long orgId) {
        // ...
    }
}
```

### Authorization Service

Create a custom authorization service for complex permission checks:

```java
@Service("authz")
public class AuthorizationService {

    public boolean isOrgMember(Long orgId, String requiredRole) {
        // Check if current user is a member of org with required role
    }

    public boolean isOrgMemberOrHigher(Long orgId, String minimumRole) {
        // Check role hierarchy within organization
    }

    public boolean ownsResource(String resourceType, Long resourceId) {
        // Generic resource ownership check
    }

    public boolean canManageOpportunity(Long opportunityId) {
        // Check if user can manage this opportunity via org membership
    }

    public boolean canManageRegistration(Long registrationId) {
        // Check if user owns registration or manages the opportunity's org
    }
}
```

---

## API Endpoints

### User Profile Endpoints

```
GET    /users/me                    - Get current user profile
PUT    /users/me                    - Update current user profile
PUT    /users/me/status             - Update user status (VOLUNTEER, ORG_REPRESENTATIVE, etc.)
PUT    /users/me/visibility         - Toggle profile public/private
GET    /users/public                - List public profiles (paginated)
GET    /users/public/{id}           - View a public profile
```

### Admin Endpoints

```
GET    /admin/users                 - List all users (ADMIN+)
GET    /admin/users/{id}            - View any user (ADMIN+)
GET    /admin/users/signins         - View recent sign-ins (ADMIN+)
GET    /admin/organizations         - List all organizations including unverified (ADMIN+)
PUT    /admin/organizations/{id}/verify - Verify an organization (ADMIN+)
```

### Super Admin Endpoints

```
PUT    /super-admin/users/{id}/role - Change user role (SUPER_ADMIN only)
DELETE /super-admin/users/{id}      - Delete user (SUPER_ADMIN only)
GET    /super-admin/audit-log       - View admin audit log (SUPER_ADMIN only)
```

### Organization Management Endpoints

```
GET    /organizations                      - List verified organizations
GET    /organizations/{id}                 - View organization details
POST   /organizations                      - Create organization (becomes OWNER)
PUT    /organizations/{id}                 - Update organization (OWNER/ADMIN member)
DELETE /organizations/{id}                 - Delete organization (OWNER or SUPER_ADMIN)

GET    /organizations/{id}/members         - List organization members
POST   /organizations/{id}/members         - Invite member (OWNER/ADMIN)
PUT    /organizations/{id}/members/{uid}   - Update member role (OWNER only)
DELETE /organizations/{id}/members/{uid}   - Remove member (OWNER/ADMIN)

GET    /organizations/{id}/opportunities   - List org's opportunities
GET    /organizations/{id}/registrations   - List registrations for org's opportunities
```

### Volunteer Self-Service Endpoints

```
GET    /my/registrations                   - List my registrations
POST   /opportunities/{id}/register        - Register for opportunity
DELETE /my/registrations/{id}              - Cancel my registration
GET    /my/hours                           - View my volunteer hours
POST   /my/hours                           - Log volunteer hours
```

---

## Command-Line Administration Scripts

### Directory Structure

```
backend/
├── src/main/java/.../cli/
│   ├── AdminCLI.java
│   └── commands/
│       ├── AddSuperAdminCommand.java
│       ├── RemoveSuperAdminCommand.java
│       ├── ListAdminsCommand.java
│       └── ResetUserPasswordCommand.java
└── scripts/
    ├── add-super-admin.sh
    ├── remove-super-admin.sh
    ├── list-admins.sh
    └── reset-password.sh
```

### Spring Boot CLI Runner

Create a CLI command system using Spring Boot's `CommandLineRunner`:

```java
@Component
@Profile("cli")
public class AdminCLI implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0];
        switch (command) {
            case "add-super-admin" -> addSuperAdmin(args);
            case "remove-super-admin" -> removeSuperAdmin(args);
            case "list-admins" -> listAdmins();
            case "list-users" -> listUsers(args);
            default -> printUsage();
        }
    }

    private void addSuperAdmin(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: add-super-admin <email>");
            System.exit(1);
        }
        String email = args[1];

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));

        user.setRole(UserRole.SUPER_ADMIN);
        userRepository.save(user);

        System.out.println("Successfully granted SUPER_ADMIN role to: " + email);
    }

    private void removeSuperAdmin(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: remove-super-admin <email>");
            System.exit(1);
        }
        String email = args[1];

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));

        if (user.getRole() != UserRole.SUPER_ADMIN) {
            System.err.println("User is not a SUPER_ADMIN: " + email);
            System.exit(1);
        }

        // Count remaining super admins
        long superAdminCount = userRepository.countByRole(UserRole.SUPER_ADMIN);
        if (superAdminCount <= 1) {
            System.err.println("Cannot remove the last SUPER_ADMIN");
            System.exit(1);
        }

        user.setRole(UserRole.ADMIN);
        userRepository.save(user);

        System.out.println("Successfully revoked SUPER_ADMIN role from: " + email);
    }

    private void listAdmins() {
        System.out.println("\n=== Platform Administrators ===\n");

        List<User> superAdmins = userRepository.findByRole(UserRole.SUPER_ADMIN);
        System.out.println("SUPER_ADMIN (" + superAdmins.size() + "):");
        superAdmins.forEach(u -> System.out.println("  - " + u.getEmail() + " (" + u.getFirstName() + " " + u.getLastName() + ")"));

        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        System.out.println("\nADMIN (" + admins.size() + "):");
        admins.forEach(u -> System.out.println("  - " + u.getEmail() + " (" + u.getFirstName() + " " + u.getLastName() + ")"));
    }

    private void listUsers(String[] args) {
        boolean showAll = args.length > 1 && args[1].equals("--all");

        System.out.println("\n=== Users ===\n");

        List<User> users;
        if (showAll) {
            users = userRepository.findAll();
        } else {
            // Show recent sign-ins (last 30 days)
            LocalDateTime since = LocalDateTime.now().minusDays(30);
            users = userRepository.findByLastLoginAtAfter(since);
        }

        System.out.println("Email | Name | Role | Last Login | Provider");
        System.out.println("-".repeat(80));

        users.forEach(u -> System.out.printf("%s | %s %s | %s | %s | %s%n",
            u.getEmail(),
            u.getFirstName(),
            u.getLastName(),
            u.getRole(),
            u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : "Never",
            u.getAuthProvider()
        ));

        System.out.println("\nTotal: " + users.size() + " users");
    }
}
```

### Shell Scripts

**scripts/add-super-admin.sh**
```bash
#!/bin/bash
# Add super admin by email

if [ -z "$1" ]; then
    echo "Usage: $0 <email>"
    exit 1
fi

cd "$(dirname "$0")/.."
./mvnw spring-boot:run -Dspring-boot.run.profiles=cli -Dspring-boot.run.arguments="add-super-admin $1"
```

**scripts/remove-super-admin.sh**
```bash
#!/bin/bash
# Remove super admin role from user

if [ -z "$1" ]; then
    echo "Usage: $0 <email>"
    exit 1
fi

cd "$(dirname "$0")/.."
./mvnw spring-boot:run -Dspring-boot.run.profiles=cli -Dspring-boot.run.arguments="remove-super-admin $1"
```

**scripts/list-admins.sh**
```bash
#!/bin/bash
# List all platform administrators

cd "$(dirname "$0")/.."
./mvnw spring-boot:run -Dspring-boot.run.profiles=cli -Dspring-boot.run.arguments="list-admins"
```

**scripts/list-users.sh**
```bash
#!/bin/bash
# List users (recent sign-ins by default, --all for all users)

cd "$(dirname "$0")/.."
./mvnw spring-boot:run -Dspring-boot.run.profiles=cli -Dspring-boot.run.arguments="list-users $1"
```

---

## User Profile Management

### Profile Visibility

Users can make their profile public or private:

```java
@Entity
public class User {
    // ... existing fields

    @Column(nullable = false)
    private Boolean isPublic = false;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus userStatus = UserStatus.VOLUNTEER;

    @Column(length = 1000)
    private String bio;

    @Column(length = 500)
    private String avatarUrl;

    private LocalDateTime lastLoginAt;
}
```

### Public Profile DTO

```java
public record PublicProfileDTO(
    Long id,
    String firstName,
    String lastName,
    String bio,
    String avatarUrl,
    UserStatus userStatus,
    Integer totalVolunteerHours,  // Computed from verified hours
    List<String> skills,          // User's skills/interests
    LocalDateTime memberSince
) {}
```

### Profile Endpoints Implementation

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(@AuthenticationPrincipal UserDetails user) {
        // Return full profile for current user
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody UpdateProfileDTO dto) {
        // Update profile fields (name, bio, avatar, etc.)
    }

    @PutMapping("/me/status")
    public ResponseEntity<UserProfileDTO> updateStatus(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody UpdateStatusDTO dto) {
        // Update user status (VOLUNTEER, ORG_REPRESENTATIVE, BOTH, INACTIVE)
    }

    @PutMapping("/me/visibility")
    public ResponseEntity<UserProfileDTO> toggleVisibility(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody VisibilityDTO dto) {
        // Toggle isPublic flag
    }

    @GetMapping("/public")
    public ResponseEntity<Page<PublicProfileDTO>> getPublicProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Return paginated list of public profiles
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<PublicProfileDTO> getPublicProfile(@PathVariable Long id) {
        // Return public profile or 404 if not public
    }
}
```

---

## Implementation Plan

### Phase 1: Database Schema & Core Models
1. Create Flyway migrations for new columns and tables
2. Update User entity with new fields
3. Create OrganizationMember entity
4. Create AdminAuditLog entity
5. Add UserStatus enum

### Phase 2: Authorization Infrastructure
1. Add SUPER_ADMIN to UserRole enum
2. Create AuthorizationService with permission checks
3. Update SecurityConfig with role-based rules
4. Add @PreAuthorize annotations to existing services
5. Create JwtAuthenticationFilter improvements to load full user

### Phase 3: CLI Administration Tools
1. Create AdminCLI component with cli profile
2. Implement add-super-admin command
3. Implement remove-super-admin command
4. Implement list-admins command
5. Implement list-users command
6. Create shell wrapper scripts
7. Add UserRepository methods (countByRole, findByRole, findByLastLoginAtAfter)

### Phase 4: User Profile Management
1. Create UserController with profile endpoints
2. Implement profile visibility toggle
3. Implement user status management
4. Create public profile listing and detail views
5. Add DTOs for profile operations

### Phase 5: Organization Authorization
1. Create OrganizationMemberRepository
2. Update OrganizationService with member management
3. Add permission checks to organization operations
4. Create organization member invitation flow

### Phase 6: Admin Endpoints
1. Create AdminController for user listing
2. Implement sign-in history viewing
3. Create SuperAdminController for elevated operations
4. Implement audit logging for admin actions

### Phase 7: Frontend Integration
1. Add role-based UI rendering
2. Create admin dashboard pages
3. Add profile settings page
4. Implement organization member management UI

---

## Security Considerations

1. **Role Escalation Prevention** - Only SUPER_ADMIN can grant admin roles
2. **Last Admin Protection** - Cannot remove the last SUPER_ADMIN
3. **Audit Logging** - All admin actions are logged with IP address
4. **Resource Ownership** - All mutations verify ownership before proceeding
5. **Public Profile Data** - Only expose safe fields in public profiles
6. **CLI Access** - CLI commands require database access (not exposed via API)

---

## Testing Strategy

1. **Unit Tests** - AuthorizationService permission logic
2. **Integration Tests** - API endpoint authorization with different roles
3. **CLI Tests** - Admin command execution and validation
4. **Security Tests** - Attempt unauthorized access with various roles
