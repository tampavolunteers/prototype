# Admin Interfaces Design

> Covers two new administrative surfaces: the **Super Admin org management panel** (enhancement to the existing `/admin` dashboard) and the **Org Dashboard** (new page for org admins to manage their own organizations).

---

## Scope

| Surface | Who sees it | Route |
|---------|-------------|-------|
| Admin Dashboard → Organizations tab (enhanced) | `ADMIN`, `SUPER_ADMIN` | `/admin` (existing) |
| Org Dashboard (list) | `ORG_ADMIN` (not `ADMIN`/`SUPER_ADMIN`, who use `/admin`) | `/org-dashboard` (new) |
| Org Dashboard (detail) | `ORG_ADMIN` (not `ADMIN`/`SUPER_ADMIN`) | `/org-dashboard/:id` (new) |

---

## 1. Backend Changes

### 1.1 New DTOs

#### `OrgMemberDTO`
The existing `GET /organizations/{id}/members` returns raw `OrganizationMember` entities with lazy-loaded `User` and `Organization` associations — callers cannot use name/email without triggering additional queries or risking serialization issues. A flat DTO is needed.

```java
public record OrgMemberDTO(
    Long id,
    Long userId,
    String email,
    String firstName,
    String lastName,
    String role,          // OWNER | ADMIN | MEMBER
    LocalDateTime joinedAt,
    String invitedByEmail // nullable
) {}
```

#### `MyOrgDTO`
For the org admin "my organizations" list — includes the caller's own role within each org.

```java
public record MyOrgDTO(
    Long id,
    String name,
    String description,
    String contactEmail,
    String city,
    String state,
    Boolean verified,
    String myRole,        // OWNER | ADMIN
    LocalDateTime createdAt
) {}
```

> `UserLookupDTO` is deferred with the invite flow to a future pass.

---

### 1.2 Updated Endpoints

#### `GET /organizations/{id}/members` — return type change
Currently returns `List<OrganizationMember>`. Change to `List<OrgMemberDTO>`.
Service change: `OrganizationService.getMembers()` maps each member to `OrgMemberDTO` by reading `member.getUser()` within the same transaction.

#### `GET /organizations/my` — new endpoint
Returns organizations where the authenticated user holds `OWNER` or `ADMIN` membership.

```
GET /organizations/my
Auth: any authenticated user
Response: List<MyOrgDTO>
```

Service: queries `OrganizationMemberRepository.findByUserIdAndRoleIn(currentUser.getId(), [OWNER, ADMIN])`, maps each to `MyOrgDTO` using both the org fields and the member's own role.

---

### 1.3 Repository change

`OrganizationMemberRepository` needs one additional derived query:

```java
List<OrganizationMember> findByUserIdAndRoleIn(Long userId, List<OrgMemberRole> roles);
```

---

## 2. Frontend Changes

### 2.1 `authService.js` — new methods

```js
getOrgMembers(orgId)                        // GET /organizations/{id}/members
updateOrgMemberRole(orgId, userId, role)    // PUT /organizations/{id}/members/{userId}/role
removeOrgMember(orgId, userId)              // DELETE /organizations/{id}/members/{userId}
deleteOrganization(id)                      // DELETE /organizations/{id}
getMyOrganizations()                        // GET /organizations/my
```

> `addOrgMember` and `lookupUserByEmail` are deferred with the invite flow.

---

### 2.2 Enhanced Admin Dashboard — Organizations Tab

The current Organizations tab has a table with a single "Verify" action. We add a **"Members" button** per row that opens an `OrgMembersModal`.

#### OrgMembersModal

A full-screen overlay modal with two sections:

**Header / Org Info strip**
- Name, location, verified badge, contact email

**Members table**

Columns: Name, Email, Org Role, Joined, Actions

The Actions column behavior differs by platform role:

| Action | `ADMIN` (platform) | `SUPER_ADMIN` |
|--------|-------------------|---------------|
| View member list | ✓ | ✓ |
| Promote non-OWNER to ADMIN / demote ADMIN to MEMBER | ✓ | ✓ |
| Remove MEMBER or ADMIN | ✓ | ✓ |
| Remove or change role of OWNER | — | ✓ |

- **Role dropdown** (ADMIN/MEMBER): shown for all non-OWNER rows for `ADMIN`+; OWNER row is locked.  Saves via `PUT /organizations/{id}/members/{userId}/role`.
- **Remove button**: shown for all non-OWNER rows for `ADMIN`+; disabled on OWNER row (SUPER_ADMIN can remove OWNER).

> Invite Member form is **out of scope** for this pass — a future expansion.

---

### 2.3 New Page: `OrgDashboard.jsx` (`/org-dashboard`)

The list view. Accessible to `ORG_ADMIN` users who are not also `ADMIN`/`SUPER_ADMIN`.

#### Layout

```
My Organizations

┌──────────────────────────────────────────────────────────┐
│ Riverside Food Bank                    ● Verified  OWNER  │
│ Tampa, FL · contact@riverside.org                         │
│                                   [View] [Manage ▾]       │
├──────────────────────────────────────────────────────────┤
│  Members (3)                              ← expanded panel│
│  Name         Email           Role    Joined   Actions    │
│  Jane Smith   jane@...        OWNER   Jan 25   —          │
│  Bob Jones    bob@...         ADMIN   Feb 25   [MEMBER▾][×]│
│  Alice Wu     alice@...       MEMBER  Mar 25   [ADMIN▾][×] │
│                                                           │
│  ⚠ [Delete Organization]  (OWNER only, red, confirm)      │
└──────────────────────────────────────────────────────────┘
```

- **[View]** navigates to `/org-dashboard/:id`.
- **[Manage ▾]** toggles the inline member panel on the list card. Intended for quick single-action edits.
- The inline panel shows the same member table as the detail page (see §2.4) but without the page chrome.

#### Permission rules (Org Dashboard, driven by `myRole` from `MyOrgDTO`)

| Action | `myRole = OWNER` | `myRole = ADMIN` |
|--------|-----------------|-----------------|
| See member list | ✓ | ✓ |
| Promote MEMBER → ADMIN / demote ADMIN → MEMBER | ✓ | — |
| Remove MEMBER or ADMIN | ✓ | ✓ |
| Remove OWNER | — | — |
| Delete org | ✓ | — |

#### Delete org confirmation
Clicking "Delete Organization" shows an inline confirmation prompt: type the org name to confirm. Submits `DELETE /organizations/{id}` on match.

---

### 2.4 New Page: `OrgDetail.jsx` (`/org-dashboard/:id`)

The full detail view for a single org. Reachable via [View] on the list card or by direct URL.

#### Layout

```
← My Organizations

Riverside Food Bank                               ● Verified
Tampa, FL · contact@riverside.org · riverside.org

─────────────────────────────────────────────────────────────
Members                                           Your role: OWNER

Name           Email              Org Role   Joined      Actions
Jane Smith     jane@...           OWNER      Jan 2025    —
Bob Jones      bob@...            ADMIN      Feb 2025    [MEMBER ▾] [Remove]
Alice Wu       alice@...          MEMBER     Mar 2025    [ADMIN ▾]  [Remove]
─────────────────────────────────────────────────────────────

                                         ⚠ [Delete Organization]
```

- Breadcrumb "← My Organizations" links back to `/org-dashboard`.
- Member table uses the same permission logic as §2.3.
- Role dropdown and Remove button call the same endpoints.
- Delete button only rendered when `myRole === 'OWNER'`, with the same inline name-confirmation as the list view.

---

### 2.5 Route Guard — `OrgAdminRoute`

New component alongside `AdminRoute.jsx`:

```jsx
// components/OrgAdminRoute.jsx
const OrgAdminRoute = ({ children }) => {
  const { isAuthenticated, hasRole, loading } = useAuth();
  if (loading) return null;
  if (!isAuthenticated) return <Navigate to="/login" />;
  if (!hasRole('ORG_ADMIN')) return <Navigate to="/dashboard" />;
  return children;
};
```

---

### 2.6 `App.jsx` — new routes

```jsx
<Route path="/org-dashboard" element={<OrgAdminRoute><OrgDashboard /></OrgAdminRoute>} />
<Route path="/org-dashboard/:id" element={<OrgAdminRoute><OrgDetail /></OrgAdminRoute>} />
```

---

### 2.7 `Navbar.jsx` — new link

Visible only to `ORG_ADMIN` users who do not also hold `ADMIN`/`SUPER_ADMIN` (those already have the "Admin" link):

```jsx
{hasRole('ORG_ADMIN') && !hasRole('ADMIN') && (
  <Link to="/org-dashboard">My Org</Link>
)}
```

---

## 3. File Change Summary

| File | Change |
|------|--------|
| `dto/OrgMemberDTO.java` | New |
| `dto/MyOrgDTO.java` | New |
| `repository/OrganizationMemberRepository.java` | Add `findByUserIdAndRoleIn` |
| `service/OrganizationService.java` | Update `getMembers` return type; add `getMyOrganizations` |
| `controller/OrganizationController.java` | Update `getMembers` return type; add `GET /organizations/my` |
| `services/authService.js` | Add 5 new methods |
| `pages/AdminDashboard.jsx` | Add "Members" button + `OrgMembersModal` component |
| `pages/OrgDashboard.jsx` | New page (list view) |
| `pages/OrgDetail.jsx` | New page (detail view) |
| `components/OrgAdminRoute.jsx` | New route guard |
| `App.jsx` | Add `/org-dashboard` and `/org-dashboard/:id` routes |
| `Navbar.jsx` | Add "My Org" link for `ORG_ADMIN` (non-admin) users |

---

## 4. Out of Scope (deferred)

- **Invite member flow** — requires `UserLookupDTO`, `GET /users/lookup`, and invite UI; future pass
- Creating or editing org details (name, address, contact) from either dashboard
- Opportunity management from the Org Dashboard
- Registration/hours management from the Org Dashboard
- Ownership transfer UI
