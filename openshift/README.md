# OpenShift Deployment Guide

This guide covers setting up and deploying the Tampa Volunteers platform on OpenShift/OKD.

## Prerequisites

- Access to an OpenShift/OKD cluster
- OpenShift CLI (`oc`) installed
- Git repository access for source builds

## Deploying the Latest Build

Images are built automatically by GitHub Actions on every push to `main` and stored in `ghcr.io`. To deploy the latest build:

```bash
cd openshift
./setup-openshift.sh --deploy
```

| Command | Purpose |
|---|---|
| `./setup-openshift.sh --deploy` | Deploy latest images from ghcr.io |
| `./setup-openshift.sh --status` | Show pods, services, and routes |
| `oc get pods -w` | Watch pod startup in real time |
| `oc rollout restart deployment/backend` | Force re-pull the latest image |

> **Note:** The `latest` tag is only built from the `main` branch. If you want to deploy a feature branch, merge it to `main` first, or trigger the workflow manually via GitHub Actions (`workflow_dispatch`).

---

## Quick Start

### 1. Install OpenShift CLI

**macOS:**
```bash
./scripts/install-oc-macos.sh
```

**Windows (PowerShell as Administrator):**
```powershell
.\scripts\install-oc-windows.ps1
```

**Alternative methods:**
- macOS: `brew install openshift-cli`
- Windows: `winget install RedHat.OpenShift-Client` or `choco install openshift-cli`
- Download directly from: https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/

### 2. Login to OpenShift

Get your login command from the OpenShift web console:
1. Log into the web console
2. Click your username in the top right corner
3. Select "Copy login command"
4. Run the copied command in your terminal

Or login directly:
```bash
oc login https://api.your-cluster.example.com:6443 --token=<your-token>
```

### 3. Run the Setup Script

**macOS/Linux:**
```bash
cd openshift
chmod +x setup-openshift.sh
./setup-openshift.sh
```

**Windows:**
```powershell
cd openshift
.\setup-openshift.ps1
```

The script will:
1. Verify prerequisites
2. Create the OpenShift project
3. Configure secrets (with option to generate secure values)
4. Apply Kubernetes resources
5. Set up image builds
6. Deploy the application
7. Display access URLs

## Manual Deployment

If you prefer to deploy manually:

### Create Project
```bash
oc new-project tampavolunteers \
  --display-name="Tampa Volunteers" \
  --description="Tampa Volunteers Platform"
```

### Apply Resources (in order)
```bash
# Core configuration
oc apply -f base/configmap.yaml
oc apply -f base/secret.yaml

# Database
oc apply -f base/postgres-pvc.yaml
oc apply -f base/postgres-deployment.yaml
oc apply -f base/postgres-service.yaml

# Wait for database to be ready
oc wait --for=condition=ready pod -l app.kubernetes.io/name=postgres --timeout=120s

# Backend
oc apply -f base/backend-buildconfig.yaml
oc apply -f base/backend-deployment.yaml
oc apply -f base/backend-service.yaml
oc apply -f base/backend-route.yaml

# Frontend
oc apply -f base/frontend-buildconfig.yaml
oc apply -f base/frontend-deployment.yaml
oc apply -f base/frontend-service.yaml
oc apply -f base/frontend-route.yaml
```

### Start Builds
```bash
oc start-build backend --follow
oc start-build frontend --follow
```

### Using Kustomize
```bash
oc apply -k base/
```

## Configuration

### Secrets

**IMPORTANT:** Before deploying to production, update `base/secret.yaml` with secure values:

```bash
# Generate a secure JWT secret
openssl rand -base64 48

# Generate a secure database password
openssl rand -base64 24 | tr -dc 'a-zA-Z0-9'
```

Update the values in `secret.yaml`:
- `JWT_SECRET`: Your generated JWT secret (minimum 256 bits)
- `POSTGRES_PASSWORD`: Your generated database password
- `SPRING_DATASOURCE_PASSWORD`: Same as POSTGRES_PASSWORD

### OAuth2 Credentials Setup

The application supports Google and GitHub OAuth2 authentication. You'll need to create OAuth applications with each provider and configure the credentials in `secret.yaml`.

#### Google OAuth2 Setup

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Navigate to **APIs & Services > Credentials**
   - Direct URL: https://console.cloud.google.com/apis/credentials
4. Click **Create Credentials > OAuth client ID**
5. If prompted, configure the OAuth consent screen first:
   - Go to **OAuth consent screen**
   - Select **External** (or Internal for Google Workspace)
   - Fill in the required fields (App name, User support email, Developer contact)
   - Add scopes: `email`, `profile`, `openid`
   - Add test users if in testing mode
6. Create OAuth client ID:
   - Application type: **Web application**
   - Name: `Tampa Volunteers` (or your app name)
   - Authorized JavaScript origins:
     ```
     https://frontend-tampavolunteers.apps.your-cluster.com
     ```
   - Authorized redirect URIs:
     ```
     https://backend-tampavolunteers.apps.your-cluster.com/api/oauth2/callback/google
     https://backend-tampavolunteers.apps.your-cluster.com/login/oauth2/code/google
     ```
7. Copy the **Client ID** and **Client Secret**

#### GitHub OAuth2 Setup

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
   - Direct URL: https://github.com/settings/apps
2. Click **OAuth Apps** in the left sidebar
3. Click **New OAuth App** (or **Register a new application**)
4. Fill in the application details:
   - **Application name**: `Tampa Volunteers` (or your app name)
   - **Homepage URL**:
     ```
     https://frontend-tampavolunteers.apps.your-cluster.com
     ```
   - **Authorization callback URL**:
     ```
     https://backend-tampavolunteers.apps.your-cluster.com/api/oauth2/callback/github
     ```
5. Click **Register application**
6. Copy the **Client ID**
7. Click **Generate a new client secret** and copy it immediately (it won't be shown again)

#### Configure OAuth Secrets in OpenShift

Update `base/secret.yaml` with your OAuth credentials:

```yaml
stringData:
  # ... other secrets ...

  # Google OAuth2 credentials
  GOOGLE_CLIENT_ID: "your-google-client-id.apps.googleusercontent.com"
  GOOGLE_CLIENT_SECRET: "your-google-client-secret"

  # GitHub OAuth2 credentials
  GITHUB_CLIENT_ID: "your-github-client-id"
  GITHUB_CLIENT_SECRET: "your-github-client-secret"
```

After updating the secrets, apply them:
```bash
oc apply -f base/secret.yaml

# Restart the backend to pick up the new secrets
oc rollout restart deployment/backend
```

**Important Notes:**
- Replace `your-cluster.com` with your actual OpenShift cluster domain
- Get your actual route URLs with: `oc get routes`
- Keep your client secrets secure - never commit them to version control
- For production, consider using OpenShift's built-in secret management or external secret stores

### ConfigMap

Edit `base/configmap.yaml` to customize:
- `SPRING_PROFILES_ACTIVE`: Spring profile (dev/prod)
- `VITE_APP_NAME`: Application display name

### Routes

Routes are auto-configured by OpenShift. To use custom hostnames, edit:
- `base/backend-route.yaml`: Uncomment and set `spec.host`
- `base/frontend-route.yaml`: Uncomment and set `spec.host`

### Build Configuration

Update the Git repository URL in:
- `base/backend-buildconfig.yaml`
- `base/frontend-buildconfig.yaml`

Change `spec.source.git.uri` to your repository URL.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     OpenShift Cluster                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                 tampavolunteers namespace               │   │
│  │                                                          │   │
│  │   ┌──────────┐    ┌──────────┐    ┌──────────────────┐  │   │
│  │   │ Frontend │    │ Backend  │    │    PostgreSQL    │  │   │
│  │   │  (Nginx) │───▶│ (Spring) │───▶│    (Database)    │  │   │
│  │   └────┬─────┘    └────┬─────┘    └────────┬─────────┘  │   │
│  │        │               │                    │            │   │
│  │   ┌────┴─────┐    ┌────┴─────┐         ┌───┴────┐       │   │
│  │   │  Route   │    │  Route   │         │  PVC   │       │   │
│  │   │ (HTTPS)  │    │ (/api)   │         │ (5Gi)  │       │   │
│  │   └────┬─────┘    └────┬─────┘         └────────┘       │   │
│  └────────┼───────────────┼────────────────────────────────┘   │
│           │               │                                     │
└───────────┼───────────────┼─────────────────────────────────────┘
            │               │
       ┌────┴───────────────┴────┐
       │      External Access    │
       │   https://frontend-*    │
       │   https://backend-*/api │
       └─────────────────────────┘
```

## Monitoring

### View Pods
```bash
oc get pods -w
```

### View Logs
```bash
# Backend logs
oc logs -f deployment/backend

# Frontend logs
oc logs -f deployment/frontend

# Database logs
oc logs -f deployment/postgres
```

### Build Logs
```bash
oc logs -f bc/backend
oc logs -f bc/frontend
```

### Check Routes
```bash
oc get routes
```

## Troubleshooting

### Pods not starting
```bash
# Check pod status
oc describe pod <pod-name>

# Check events
oc get events --sort-by='.lastTimestamp'
```

### Build failures
```bash
# Check build logs
oc logs bc/backend
oc logs bc/frontend

# Restart a build
oc start-build backend
```

### Database connection issues
```bash
# Verify postgres is running
oc get pods -l app.kubernetes.io/name=postgres

# Check postgres logs
oc logs deployment/postgres

# Test connectivity from backend
oc exec deployment/backend -- nc -zv postgres 5432
```

### Image pull errors
Ensure the ImageStreams exist and have images:
```bash
oc get is
oc describe is backend
oc describe is frontend
```

## Scaling

### Scale deployments
```bash
# Scale backend
oc scale deployment/backend --replicas=3

# Scale frontend
oc scale deployment/frontend --replicas=2
```

### Auto-scaling (HPA)
```bash
oc autoscale deployment/backend --min=2 --max=5 --cpu-percent=80
```

## Cleanup

### Delete all resources
```bash
oc delete project tampavolunteers
```

### Delete specific resources
```bash
oc delete deployment,svc,route,pvc -l app.kubernetes.io/part-of=tampavolunteers-platform
```

## File Structure

```
openshift/
├── README.md                    # This file
├── setup-openshift.sh           # Setup script (macOS/Linux)
├── setup-openshift.ps1          # Setup script (Windows)
├── scripts/
│   ├── install-oc-macos.sh      # CLI installer for macOS
│   └── install-oc-windows.ps1   # CLI installer for Windows
└── base/
    ├── kustomization.yaml       # Kustomize configuration
    ├── namespace.yaml           # Namespace definition
    ├── configmap.yaml           # Application configuration
    ├── secret.yaml              # Sensitive configuration
    ├── postgres-pvc.yaml        # Database storage
    ├── postgres-deployment.yaml # Database deployment
    ├── postgres-service.yaml    # Database service
    ├── backend-deployment.yaml  # Backend deployment
    ├── backend-service.yaml     # Backend service
    ├── backend-route.yaml       # Backend route
    ├── backend-buildconfig.yaml # Backend build config
    ├── frontend-deployment.yaml # Frontend deployment
    ├── frontend-service.yaml    # Frontend service
    ├── frontend-route.yaml      # Frontend route
    └── frontend-buildconfig.yaml # Frontend build config
```

## Additional Resources

- [OKD Documentation](https://docs.okd.io/)
- [OpenShift Documentation](https://docs.openshift.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [oc CLI Reference](https://docs.openshift.com/container-platform/latest/cli_reference/openshift_cli/getting-started-cli.html)
