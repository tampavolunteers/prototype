# Tampa Volunteers — Feature Reference

## Table of Contents
1. [Role Permissions Matrix](#role-permissions-matrix)
2. [API Endpoints](#api-endpoints)
   - [Authentication](#authentication)
   - [User Profile](#user-profile)
   - [Organizations](#organizations)
   - [Admin](#admin)
   - [Super Admin](#super-admin)
3. [CLI Administration Tools](#cli-administration-tools)
4. [Bootstrapping the First Super Admin](#bootstrapping-the-first-super-admin)

---

## Role Permissions Matrix

| Action | VOLUNTEER | ORG_ADMIN | ADMIN | SUPER_ADMIN |
|--------|-----------|-----------|-------|-------------|
| View public opportunities | ✓ | ✓ | ✓ | ✓ |
| Register for opportunities | ✓ | ✓ | ✓ | ✓ |
| View own profile | ✓ | ✓ | ✓ | ✓ |
| Update own profile | ✓ | ✓ | ✓ | ✓ |
| View public profiles | ✓ | ✓ | ✓ | ✓ |
| Create organization | ✓ | ✓ | ✓ | ✓ |
| Manage own organization | — | OWNER/ADMIN | ✓ | ✓ |
| Invite org members | — | OWNER/ADMIN | ✓ | ✓ |
| Update member roles | — | OWNER only | ✓ | ✓ |
| List all users (paginated) | — | — | ✓ | ✓ |
| View any user | — | — | ✓ | ✓ |
| View all organizations | — | — | ✓ | ✓ |
| Verify organization | — | — | ✓ | ✓ |
| Change any user's role | — | — | — | ✓ |
| Delete any user | — | — | — | ✓ |
| View audit log | — | — | — | ✓ |

> **Cumulative authorities:** SUPER_ADMIN includes all ADMIN, ORG_ADMIN, and VOLUNTEER permissions.

---

## API Endpoints

All endpoints are prefixed with the configured context path (default: `/api`).

### Opportunities

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/opportunities` | Public | Paginated list of published opportunities (filters: `keyword`, `categoryId`, `organizationId`, `startDate`, `endDate`, `page`, `size`) |
| GET | `/opportunities/{id}` | Public | Get single opportunity |

### Categories

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/categories` | Public | List all categories |

---

### Authentication

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | Public | Register new user |
| POST | `/auth/login` | Public | Login (returns JWT) |
| GET | `/auth/me` | JWT | Get current user info |
| GET | `/oauth2/authorization/google` | Public | Start Google OAuth |
| GET | `/oauth2/authorization/github` | Public | Start GitHub OAuth |

**Response fields for login/register:** `id`, `token`, `email`, `firstName`, `lastName`, `role`

---

### User Profile

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/users/me` | JWT | Get full profile of current user |
| PUT | `/users/me` | JWT | Update profile (firstName, lastName, bio, avatarUrl, phone) |
| PUT | `/users/me/status` | JWT | Update user status (VOLUNTEER, ORG_REPRESENTATIVE, BOTH, INACTIVE) |
| PUT | `/users/me/visibility` | JWT | Toggle public profile (isPublic: true/false) |
| GET | `/users/public` | Public | Paginated list of public profiles |
| GET | `/users/public/{id}` | Public | Single public profile |

**User Status values:** `VOLUNTEER`, `ORG_REPRESENTATIVE`, `BOTH`, `INACTIVE`

---

### Organizations

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/organizations` | Public | List verified organizations |
| GET | `/organizations/{id}` | Public | Get single organization |
| POST | `/organizations` | JWT | Create organization (creator becomes OWNER) |
| PUT | `/organizations/{id}` | JWT (OWNER/ADMIN/SUPER_ADMIN) | Update organization |
| DELETE | `/organizations/{id}` | JWT (OWNER/SUPER_ADMIN) | Delete organization |
| GET | `/organizations/{id}/members` | JWT | List organization members |
| POST | `/organizations/{id}/members` | JWT (OWNER/ADMIN) | Invite a member |
| PUT | `/organizations/{id}/members/{userId}/role` | JWT (OWNER) | Update member role |
| DELETE | `/organizations/{id}/members/{userId}` | JWT (OWNER/ADMIN) | Remove member |

**Member roles:** `OWNER`, `ADMIN`, `MEMBER`

---

### Admin

> Requires `ADMIN` or `SUPER_ADMIN` role.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/admin/users` | ADMIN | Paginated user list (all users) |
| GET | `/admin/users/{id}` | ADMIN | Get any user by ID |
| GET | `/admin/users/signins` | ADMIN | Users who signed in within last 30 days |
| GET | `/admin/organizations` | ADMIN | All organizations (including unverified) |
| PUT | `/admin/organizations/{id}/verify` | ADMIN | Verify an organization |

---

### Super Admin

> Requires `SUPER_ADMIN` role.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| PUT | `/super-admin/users/{id}/role` | SUPER_ADMIN | Change a user's role |
| DELETE | `/super-admin/users/{id}` | SUPER_ADMIN | Delete a user |
| GET | `/super-admin/audit-log` | SUPER_ADMIN | Paginated admin audit log |

---

## CLI Administration Tools

Located in `tampavolunteers-backend/scripts/`.

### Prerequisites

The database must be accessible with the same configuration as the running application. CLI commands connect to the database directly using Spring Data JPA.

### Commands

#### Add a Super Admin

```bash
./scripts/add-super-admin.sh user@example.com
```

Promotes an existing user to `SUPER_ADMIN`. The user must already be registered.

#### Remove a Super Admin

```bash
./scripts/remove-super-admin.sh user@example.com
```

Downgrades a `SUPER_ADMIN` to `ADMIN`. Fails if this is the last `SUPER_ADMIN`.

#### List Admins

```bash
./scripts/list-admins.sh
```

Lists all `ADMIN` and `SUPER_ADMIN` users.

#### List Users

```bash
# Volunteers only (default)
./scripts/list-users.sh

# All users
./scripts/list-users.sh --all
```

---

## Development Seeding

The CLI includes three seed commands for populating a development environment with realistic fake data. They must be run **in order** because opportunities depend on organizations, and organizations need at least one user.

> **Seeded records are flagged.** Every record created by a seed command has `seeded = true` in the database. Records created through the normal UI/API have `seeded = false`. This makes it easy to distinguish test data from real data.

### Seed Volunteers

```bash
./scripts/seed-volunteers.sh <count>
# Example: seed 10 volunteers
./scripts/seed-volunteers.sh 10
```

Creates `<count>` fake volunteer accounts with randomized names, emails (`@example.com`), bios, and phone numbers.

### Seed Organizations

```bash
./scripts/seed-organizations.sh <count>
# Example: seed 3 organizations
./scripts/seed-organizations.sh 3
```

Creates `<count>` fake organizations, each assigned a random existing user as owner. ~67% will be pre-verified. **Requires at least one user in the database** — run seed-volunteers first.

### Seed Opportunities

```bash
./scripts/seed-opportunities.sh <count>
# Example: seed 20 opportunities
./scripts/seed-opportunities.sh 20
```

Creates `<count>` fake published opportunities, each assigned to a random existing organization with start dates 1–90 days in the future. **Requires at least one organization in the database** — run seed-organizations first.

### Typical dev setup

```bash
./scripts/seed-volunteers.sh 10
./scripts/seed-organizations.sh 3
./scripts/seed-opportunities.sh 20
```

After seeding, open `http://localhost:5173/opportunities` to see the card grid.

---

## Bootstrapping the First Super Admin

Since the `/super-admin` API endpoints require `SUPER_ADMIN` role, and `SUPER_ADMIN` users can only be created via CLI (or direct DB), here is the bootstrap procedure for a fresh deployment:

1. **Register a user** through the normal registration flow (UI or API):
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"email":"admin@yourdomain.com","password":"SecurePass123!","firstName":"Admin","lastName":"User","role":"VOLUNTEER"}'
   ```

2. **SSH into the server** (or run locally with DB access) and run:
   ```bash
   cd tampavolunteers-backend
   ./scripts/add-super-admin.sh admin@yourdomain.com
   ```

3. **Verify** by logging in and calling:
   ```bash
   curl -H 'Authorization: Bearer <token>' http://localhost:8080/api/admin/users
   ```

4. From the Admin Dashboard (UI), additional `SUPER_ADMIN` users can be promoted via the role management interface.

> **Security note:** The CLI scripts require direct server access, which limits the risk of privilege escalation through the API alone.

---

## Database Schema Additions (V6–V9)

| Migration | Description |
|-----------|-------------|
| V6 | Added `is_public`, `user_status`, `bio`, `avatar_url`, `last_login_at` to `users` |
| V7 | Created `organization_members` table (org membership with roles) |
| V8 | Created `admin_audit_log` table (tracks admin actions) |
| V9 | Added `seeded BOOLEAN` to `organizations` and `opportunities` (existing rows backfilled to `TRUE`) |
