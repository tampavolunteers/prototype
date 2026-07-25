# Fly.io Deployment Guide

This guide covers setting up and deploying the Tampa Volunteers platform on Fly.io.

## Architecture

```
                        Fly.io Organization
  ┌───────────────────┐   internal 6PN network   ┌───────────────────┐
  │ tampavolunteers-  │──────────────────────────▶│ tampavolunteers-  │
  │ frontend (Nginx)  │  proxy /api ->            │ backend (Spring)  │
  │ public: *.fly.dev │  backend.internal:8080    │ public: *.fly.dev │
  └───────────────────┘                           └─────────┬─────────┘
                                                              │ postgres://...flycast:5432
                                                    ┌─────────▼─────────┐
                                                    │ tampavolunteers-db │
                                                    │ (Fly Postgres)     │
                                                    └────────────────────┘
```

Each component is its own Fly app: `tampavolunteers-backend`, `tampavolunteers-frontend`,
and `tampavolunteers-db` (Postgres). The frontend's Nginx proxies `/api` to the backend
over Fly's private network, so the backend never needs to be reachable from the public
internet directly (though `fly.toml` currently exposes it publicly too, matching the
OpenShift setup's separate backend route).

## Prerequisites

- A Fly.io account (`fly auth signup` / `fly auth login`)
- Fly CLI (`fly`/`flyctl`) installed

## Quick Start

### 1. Install the Fly CLI

**macOS/Linux:**
```bash
./scripts/install-flyctl-macos.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\install-flyctl-windows.ps1
```

### 2. Log in to Fly.io

```bash
fly auth login
```

### 3. Run the Setup Script

**macOS/Linux:**
```bash
cd fly
chmod +x setup-fly.sh
./setup-fly.sh
```

**Windows:**
```powershell
cd fly
.\setup-fly.ps1
```

The script will:
1. Verify prerequisites
2. Create the `tampavolunteers-backend` and `tampavolunteers-frontend` apps
3. Create a Fly Postgres cluster and attach it to the backend
4. Configure secrets (JWT, OAuth2 credentials)
5. Deploy both apps
6. Display access URLs

## Deploying the Latest Build

```bash
cd fly
./setup-fly.sh --deploy       # or: .\setup-fly.ps1 -DeployOnly
```

Or deploy a single app directly:
```bash
cd tampavolunteers-backend && fly deploy
cd tampavolunteers-frontend && fly deploy
```

| Command | Purpose |
|---|---|
| `./setup-fly.sh --deploy` | Deploy latest code to both apps |
| `./setup-fly.sh --status` | Show machine status for both apps |
| `fly logs --app tampavolunteers-backend` | Tail backend logs |
| `fly machine restart <id> --app tampavolunteers-backend` | Force a restart |

## Configuration

### Secrets

Secrets are never stored in `fly.toml` (which is committed to git). Set them with
`fly secrets set --app <app-name> KEY=value`. The setup script configures these
automatically where possible:

| Secret | App | Notes |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | backend | Derived from `fly postgres attach` |
| `JWT_SECRET` | backend | Generate with `openssl rand -base64 48` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | backend | See OAuth2 setup below |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | backend | See OAuth2 setup below |

### OAuth2 Credentials Setup

#### Google OAuth2

1. [Google Cloud Console](https://console.cloud.google.com/apis/credentials) > Create Credentials > OAuth client ID
2. Authorized JavaScript origins: `https://tampavolunteers-frontend.fly.dev`
3. Authorized redirect URIs:
   ```
   https://tampavolunteers-backend.fly.dev/api/oauth2/callback/google
   https://tampavolunteers-backend.fly.dev/login/oauth2/code/google
   ```

#### GitHub OAuth2

1. [GitHub Developer Settings](https://github.com/settings/developers) > OAuth Apps > New OAuth App
2. Homepage URL: `https://tampavolunteers-frontend.fly.dev`
3. Authorization callback URL: `https://tampavolunteers-backend.fly.dev/api/oauth2/callback/github`

Set the credentials with:
```bash
fly secrets set --app tampavolunteers-backend \
  GOOGLE_CLIENT_ID=... GOOGLE_CLIENT_SECRET=... \
  GITHUB_CLIENT_ID=... GITHUB_CLIENT_SECRET=...
```

### Custom App Names / Region

If `tampavolunteers-backend`/`tampavolunteers-frontend` are already taken on Fly (app
names are globally unique), rename the `app =` line in each `fly.toml`, update the
`tampavolunteers-backend.internal` hostname in
`tampavolunteers-frontend/nginx.conf`, and pass the same names to `setup-fly.sh`/`.ps1`
via `BACKEND_APP`/`FRONTEND_APP`/`DB_APP` env vars (or `-BackendApp` etc. on Windows).

## Database Admin CLI

Run one-off AdminCLI commands against the deployed backend:
```bash
./scripts/run-cli.sh <command> [args...]

./scripts/list-users.sh [--all]
./scripts/list-admins.sh
./scripts/add-super-admin.sh <email>
./scripts/remove-super-admin.sh <email>

./scripts/seed-volunteers.sh <count>
./scripts/seed-organizations.sh <count>
./scripts/seed-opportunities.sh <count>
./scripts/remove-seeded.sh [--volunteers] [--organizations] [--opportunities]
```

## Monitoring

```bash
fly status --app tampavolunteers-backend
fly logs --app tampavolunteers-backend
fly logs --app tampavolunteers-frontend
```

## Troubleshooting

### Machine not starting
```bash
fly status --app tampavolunteers-backend
fly logs --app tampavolunteers-backend
```

### Release command (Flyway migration) failed
```bash
fly releases --app tampavolunteers-backend
fly logs --app tampavolunteers-backend
```

### Database connection issues
```bash
fly postgres list
fly postgres connect --app tampavolunteers-db
```

### 502s from the frontend
Usually means the backend app is scaled to zero and hasn't started yet, or the
`tampavolunteers-backend.internal` hostname in `nginx.conf` doesn't match the actual
backend app name.

## Scaling

```bash
fly scale count 2 --app tampavolunteers-backend
fly scale vm shared-cpu-2x --app tampavolunteers-backend
```

## Cleanup

```bash
fly apps destroy tampavolunteers-backend
fly apps destroy tampavolunteers-frontend
fly apps destroy tampavolunteers-db
```

## File Structure

```
fly/
├── README.md                        # This file
├── setup-fly.sh                     # Setup script (macOS/Linux)
├── setup-fly.ps1                    # Setup script (Windows)
└── scripts/
    ├── install-flyctl-macos.sh      # CLI installer for macOS/Linux
    ├── install-flyctl-windows.ps1   # CLI installer for Windows
    ├── run-cli.sh                   # Run AdminCLI commands on the deployed backend
    ├── list-users.sh                # List users
    ├── list-admins.sh               # List super admins
    ├── add-super-admin.sh           # Grant super admin by email
    ├── remove-super-admin.sh        # Revoke super admin by email
    ├── seed-volunteers.sh           # Seed sample volunteers
    ├── seed-organizations.sh        # Seed sample organizations
    ├── seed-opportunities.sh        # Seed sample opportunities
    └── remove-seeded.sh             # Remove seeded data

tampavolunteers-backend/fly.toml     # Backend app config
tampavolunteers-frontend/fly.toml    # Frontend app config
```

## Additional Resources

- [Fly.io Documentation](https://fly.io/docs/)
- [Fly Postgres](https://fly.io/docs/postgres/)
- [flyctl Reference](https://fly.io/docs/flyctl/)
