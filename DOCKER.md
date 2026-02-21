# Docker Setup Guide

This document provides detailed information about using Docker with the Tampa Volunteers platform.

## Overview

The project uses Docker Compose to manage a PostgreSQL database for local development. This approach provides:

- **Consistency**: Same database version across all development environments
- **Isolation**: Database runs in a container, doesn't conflict with other projects
- **Persistence**: Data is stored in a Docker volume and survives container restarts
- **Simplicity**: Single command to start/stop the database
- **Clean teardown**: Easy to reset the database to a clean state

## Services

### PostgreSQL Database

- **Image**: `postgres:15-alpine`
- **Container Name**: `tampavolunteers-db`
- **Port**: `5432` (mapped to host)
- **Database**: `tampavolunteers`
- **Username**: `postgres`
- **Password**: `postgres`
- **Volume**: `tampavolunteers_postgres_data`

### pgAdmin (Optional)

- **Image**: `dpage/pgadmin4:latest`
- **Container Name**: `tampavolunteers-pgadmin`
- **Port**: `5050` (mapped to host)
- **Email**: `admin@tampavolunteers.local`
- **Password**: `admin`
- **Profile**: `tools` (only starts when explicitly requested)

## Usage

### Starting Services

**PostgreSQL only:**
```bash
docker-compose up -d
```

**PostgreSQL + pgAdmin:**
```bash
docker-compose --profile tools up -d
```

### Stopping Services

**Stop containers (keeps data):**
```bash
docker-compose down
```

**Stop and remove volumes (deletes all data):**
```bash
docker-compose down -v
```

### Viewing Status

**Check running containers:**
```bash
docker-compose ps
```

**View logs:**
```bash
# All services
docker-compose logs

# Specific service
docker-compose logs postgres
docker-compose logs pgadmin

# Follow logs in real-time
docker-compose logs -f postgres
```

### Database Management

**Access PostgreSQL CLI:**
```bash
docker-compose exec postgres psql -U postgres -d tampavolunteers
```

**Common psql commands:**
```sql
\l              -- List databases
\c tampavolunteers  -- Connect to database
\dt             -- List tables
\d users        -- Describe users table
\q              -- Quit
```

**Create database backup:**
```bash
docker-compose exec postgres pg_dump -U postgres tampavolunteers > backup.sql
```

**Restore database from backup:**
```bash
docker-compose exec -T postgres psql -U postgres tampavolunteers < backup.sql
```

**Reset database (clean slate):**
```bash
docker-compose down -v
docker-compose up -d
```

### Using pgAdmin

1. Start pgAdmin:
   ```bash
   docker-compose --profile tools up -d
   ```

2. Open browser to http://localhost:5050

3. Login with:
   - Email: `admin@tampavolunteers.local`
   - Password: `admin`

4. Add server connection:
   - **General > Name**: Tampa Volunteers
   - **Connection > Host**: `postgres` (container name)
   - **Connection > Port**: `5432`
   - **Connection > Username**: `postgres`
   - **Connection > Password**: `postgres`
   - **Connection > Save Password**: Yes

## Volume Management

### Inspect Volume

```bash
# List all volumes
docker volume ls

# View volume details
docker volume inspect tampavolunteers_postgres_data

# View volume location on disk
docker volume inspect tampavolunteers_postgres_data -f '{{ .Mountpoint }}'
```

### Backup Volume

```bash
# Create a backup archive
docker run --rm \
  -v tampavolunteers_postgres_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/postgres-backup.tar.gz -C /data .
```

### Restore Volume

```bash
# Restore from backup archive
docker run --rm \
  -v tampavolunteers_postgres_data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/postgres-backup.tar.gz -C /data
```

### Delete Volume

```bash
# Stop containers first
docker-compose down

# Delete volume (WARNING: deletes all data)
docker volume rm tampavolunteers_postgres_data
```

## Running Admin Scripts

The backend includes CLI scripts for managing users and roles. They run via a short-lived Docker container (the `cli` service) in the `tools` profile.

**Prerequisites:**
- Docker Compose running with at least the `postgres` service
- The `cli` image built (see below)

**Build the CLI image** (required once, and again after any backend code changes):
```bash
docker compose --profile tools build cli
```

**Available scripts (run from the project root):**
```bash
# List all users (add --all to include inactive)
./tampavolunteers-backend/scripts/list-users.sh
./tampavolunteers-backend/scripts/list-users.sh --all

# List all admins and super admins
./tampavolunteers-backend/scripts/list-admins.sh

# Promote a user to super admin
./tampavolunteers-backend/scripts/add-super-admin.sh user@example.com

# Remove super admin role from a user
./tampavolunteers-backend/scripts/remove-super-admin.sh user@example.com
```

Each script runs `docker compose --profile tools run --rm cli <command>`. The container starts, runs the command against the database, and exits — it doesn't interfere with the backend container running on port 8080.

## Troubleshooting

### CLI Script Fails: `AuthenticationManager` / `APPLICATION FAILED TO START`

The `cli` service is in the `tools` profile, so a plain `docker compose build` skips it. If the CLI image is stale or was never built after a code change, you'll see a Spring startup failure.

**Fix:** rebuild the CLI image explicitly:
```bash
docker compose --profile tools build --no-cache cli
```

To verify the new code made it into the image, copy the JAR to your host and inspect it:
```bash
docker compose --profile tools run --rm -v /tmp:/mnt --entrypoint sh cli -c "cp /app/app.jar /mnt/cli-app.jar"
unzip -l /tmp/cli-app.jar | grep AuthManager
# Should print: BOOT-INF/classes/org/tampavolunteers/config/AuthManagerConfig.class
```

If that class is missing, the build didn't pick up the latest source — re-run the build command above.

### Port 5432 Already in Use

If you have PostgreSQL installed locally:

**Option 1: Stop local PostgreSQL**
```bash
# Windows
net stop postgresql-x64-14

# Mac
brew services stop postgresql

# Linux
sudo systemctl stop postgresql
```

**Option 2: Change Docker port**

Edit `docker-compose.yml`:
```yaml
ports:
  - "5433:5432"  # Use 5433 on host instead
```

Then update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/tampavolunteers
```

### Container Won't Start

**Check logs:**
```bash
docker-compose logs postgres
```

**Common issues:**
- Port conflict (see above)
- Insufficient disk space
- Corrupted volume (try `docker-compose down -v` and restart)

### Permission Errors

**Linux only:**
```bash
# Fix volume permissions
docker-compose exec postgres chown -R postgres:postgres /var/lib/postgresql/data
```

### Connection Refused

1. Verify container is running:
   ```bash
   docker-compose ps
   ```

2. Check health status:
   ```bash
   docker-compose exec postgres pg_isready -U postgres
   ```

3. Test connection:
   ```bash
   docker-compose exec postgres psql -U postgres -d tampavolunteers -c "SELECT 1"
   ```

### Cannot Connect from Backend

1. Verify connection string in `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/tampavolunteers
   ```

2. Ensure database is running:
   ```bash
   docker-compose ps
   ```

3. Check network connectivity:
   ```bash
   telnet localhost 5432
   # or
   nc -zv localhost 5432
   ```

## Production Considerations

**⚠️ WARNING**: This Docker setup is for local development only!

For production, you should:

1. **Use a managed database service** (AWS RDS, Google Cloud SQL, Azure Database)
2. **Use strong passwords** and rotate them regularly
3. **Enable SSL/TLS** connections
4. **Configure proper backups** with retention policies
5. **Set up monitoring** and alerting
6. **Use proper secrets management** (not environment variables in docker-compose.yml)
7. **Configure resource limits** (CPU, memory)
8. **Enable database replication** for high availability
9. **Implement proper network security** (firewalls, VPNs)
10. **Follow the principle of least privilege** for database users

## Alternative: Docker Desktop GUI

If you prefer a graphical interface:

1. Open Docker Desktop
2. Go to "Containers" tab
3. You'll see `tampavolunteers-db` and optionally `tampavolunteers-pgadmin`
4. Click to view logs, stats, terminal, etc.

## Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)
- [pgAdmin Documentation](https://www.pgadmin.org/docs/)
- [Docker Volume Management](https://docs.docker.com/storage/volumes/)
