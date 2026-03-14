# Addendum: Roles vs. Status — Clarification and Org Representative Management

> This document supplements `AUTHORIZATION_DESIGN.md` and `DATA_MODEL.md` and supersedes any ambiguities between them regarding `UserRole`, `UserStatus`, and `OrgMemberRole`.

---

## The Core Distinction: `role` vs. `user_status`

These two fields on the `users` table serve fundamentally different purposes and must not be conflated.

### `role` — Platform-Wide Security Level

`UserRole` is the security principal. It controls what API endpoints the user can reach and what platform-wide data they can access. It is set by the system (or by a `SUPER_ADMIN` via CLI/admin endpoint) — **never self-assigned by the user**.

| Value | Who sets it | Meaning |
|-------|-------------|---------|
| `VOLUNTEER` | System (default on signup) | Can browse and register for opportunities |
| `ORG_ADMIN` | System (on first org creation or first org ADMIN invite) | Can create/manage orgs and their opportunities |
| `ADMIN` | `SUPER_ADMIN` via CLI | Platform moderator |
| `SUPER_ADMIN` | `SUPER_ADMIN` via CLI | Full access |

### `user_status` — Self-Declared Participation Mode

`UserStatus` describes how the user presents themselves on the platform. It is **user-controlled** (via `PUT /users/me/status`) and affects profile display and UI navigation, not security checks.

| Value | Meaning | UI effect |
|-------|---------|-----------|
| `VOLUNTEER` | Actively looking to volunteer (default) | Shows volunteer-focused dashboard |
| `ORG_REPRESENTATIVE` | Represents one or more organizations | Shows org management UI |
| `BOTH` | Volunteer and org representative | Shows combined dashboard |
| `INACTIVE` | Not currently participating | Minimal UI |

**Setting `user_status = ORG_REPRESENTATIVE` does not grant any org access by itself.** Actual org permissions are always determined by the user's row in `organization_members`.

---

## Organization Representative Lifecycle

### Creating an Organization

When a user (any `UserRole`) creates an organization:

1. An `organization_members` row is inserted with `role = OWNER`.
2. The user's platform `role` is promoted to at least `ORG_ADMIN` (no-op if already higher).
3. The user's `user_status` is set to `ORG_REPRESENTATIVE` (or `BOTH` if it was `VOLUNTEER`).

The creator is the canonical **Owner** of that organization.

### Inviting an Org Admin (Organization Representative)

An OWNER or existing ADMIN of an org may invite another platform user and grant them the `ADMIN` org-member role. This is the mechanism by which a user becomes an "Organization Representative" of that org.

When a user accepts an org `ADMIN` invitation:

1. An `organization_members` row is inserted with `role = ADMIN`.
2. The user's platform `role` is promoted to at least `ORG_ADMIN`.
3. The user's `user_status` is set to `ORG_REPRESENTATIVE` (or `BOTH` if it was `VOLUNTEER`).

> **An ADMIN member can invite new members** (`MEMBER` or `ADMIN` role) but **only the OWNER can promote an existing MEMBER to ADMIN** or demote an ADMIN back to MEMBER.

### What an Org ADMIN (Organization Representative) Can Do

Within their organization, an `ADMIN` org-member can:

- Create, edit, publish, cancel, and complete opportunities
- View all registrations for the org's opportunities
- Confirm attendance and verify volunteer hours
- Invite new MEMBER or ADMIN org-members
- Remove any non-OWNER member from the org

An `ADMIN` org-member **cannot**:

- Delete the organization
- Remove or demote the OWNER
- Transfer ownership

### What Only the OWNER Can Do

| Action | OWNER | ADMIN | MEMBER |
|--------|-------|-------|--------|
| Delete the organization | ✓ | — | — |
| Transfer ownership to another member | ✓ | — | — |
| Promote a MEMBER to ADMIN | ✓ | — | — |
| Demote an ADMIN to MEMBER | ✓ | — | — |
| Remove the OWNER themselves (self-remove) | ✓* | — | — |
| All ADMIN actions | ✓ | ✓ | — |

> *An OWNER may only remove themselves if there is at least one other OWNER, or if the org is being deleted. An org cannot be left without an OWNER.

---

## Ownership Transfer

If the current OWNER wants to step down:

1. `PUT /organizations/{id}/members/{uid}` with `role = OWNER` — promotes the target member to OWNER.
2. The original OWNER's `OrgMemberRole` is automatically demoted to `ADMIN`.

There must always be exactly one OWNER per organization. Ownership transfer is an atomic operation.

> The `organizations.user_id` column records the original creator for historical purposes but does **not** track current ownership — `organization_members` is authoritative for current roles.

---

## Role Cleanup on Org Departure or Dissolution

### When a user leaves all orgs (or is removed from all orgs):

- Their `organization_members` records are deleted.
- Their platform `role` is demoted back to `VOLUNTEER` **only if** they no longer hold `ADMIN` or `OWNER` in any org.
- Their `user_status` is set back to `VOLUNTEER` **only if** it was `ORG_REPRESENTATIVE` (if it was `BOTH`, it reverts to `VOLUNTEER`).

### When an organization is deleted:

- All `organization_members` rows for the org are cascade-deleted.
- The cleanup above applies to each affected user.

---

## Summary: Who Can Add/Remove Org Representatives

| Actor | Can invite MEMBER | Can invite ADMIN | Can remove MEMBER | Can remove ADMIN | Can remove OWNER |
|-------|-------------------|------------------|-------------------|------------------|------------------|
| OWNER | ✓ | ✓ | ✓ | ✓ | — (self only, with successor) |
| ADMIN | ✓ | ✓ | ✓ | — | — |
| MEMBER | — | — | — | — | — |
| SUPER_ADMIN (platform) | ✓ | ✓ | ✓ | ✓ | ✓ |

---

## Invariants (Enforced by Service Layer)

1. Every organization has exactly one OWNER at all times.
2. Only the OWNER (or `SUPER_ADMIN`) may delete an organization.
3. `UserRole.ORG_ADMIN` is the minimum platform role for anyone with an active org membership of ADMIN or OWNER.
4. A user with no active org ADMIN/OWNER memberships must not hold `UserRole.ORG_ADMIN` (revoked automatically).
5. `UserStatus` never controls authorization — it is display-only.
