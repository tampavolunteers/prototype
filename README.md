# Tampa Volunteers Platform

A volunteer management platform connecting community members with local volunteer opportunities in the Tampa Bay area.

## Project Structure

```
tampavolunteers/
├── .clinerules                      # Project documentation and architecture
├── .env.example                     # Example environment variables
├── .gitignore                       # Git ignore rules
├── docker-compose.yml               # Docker Compose configuration
├── README.md                        # This file
├── start-dev.sh / .bat              # Development startup scripts
├── stop-dev.sh / .bat               # Development stop scripts
├── logs/                            # Application logs (auto-generated)
├── tampavolunteers-backend/        # Spring Boot REST API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/org/tampavolunteers/
│   │   │   │   ├── model/           # JPA entities
│   │   │   │   ├── repository/      # Data access layer
│   │   │   │   ├── service/         # Business logic
│   │   │   │   ├── controller/      # REST endpoints
│   │   │   │   ├── dto/             # Data transfer objects
│   │   │   │   ├── config/          # Spring configuration
│   │   │   │   ├── security/        # JWT & security
│   │   │   │   └── exception/       # Exception handling
│   │   │   └── resources/
│   │   │       ├── application*.properties
│   │   │       └── db/migration/    # Flyway migrations
│   │   └── test/                    # Test files
│   └── pom.xml                      # Maven configuration
└── tampavolunteers-frontend/       # Vite + React SPA
    ├── src/
    │   ├── components/              # Reusable UI components
    │   ├── pages/                   # Page components
    │   ├── services/                # API service layer
    │   ├── context/                 # React Context (state)
    │   ├── hooks/                   # Custom React hooks
    │   ├── utils/                   # Helper functions
    │   ├── types/                   # TypeScript types
    │   ├── assets/                  # Images, fonts, etc.
    │   ├── App.jsx                  # Main app component
    │   ├── main.jsx                 # Entry point
    │   └── index.css                # Global styles
    ├── public/                      # Static files
    ├── package.json                 # npm configuration
    ├── vite.config.js               # Vite configuration
    ├── tailwind.config.js           # Tailwind CSS config
    └── .env.development             # Frontend env vars
```

## Tech Stack

### Backend
- Spring Boot 3.2.0
- Spring Security + JWT Authentication
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway for database migrations
- Maven

### Frontend
- Vite
- React 18
- React Router v6
- Axios for API calls
- Tailwind CSS

## Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- Maven (included via wrapper)
- **Option A:** Docker & Docker Compose (recommended for local development)
- **Option B:** PostgreSQL 14 or higher (if not using Docker)

## Quick Start

### Automated Setup (Easiest)

Use the provided startup scripts to launch everything with one command:

**Windows:**
```bash
start-dev.bat
```

**Linux/Mac:**
```bash
chmod +x start-dev.sh stop-dev.sh
./start-dev.sh
```

This will:
1. Start PostgreSQL in Docker with persistent storage
2. Start the Spring Boot backend
3. Install dependencies and start the Vite frontend

To stop all services:
```bash
# Windows
stop-dev.bat

# Linux/Mac
./stop-dev.sh
```

## Setup Instructions

### Quick Start with Docker (Recommended)

The easiest way to get started is using Docker Compose, which includes PostgreSQL with a persistent volume:

```bash
# Start PostgreSQL database
docker-compose up -d

# The database will be available at localhost:5432
# Database: tampavolunteers
# Username: postgres
# Password: postgres
```

Optional: Start pgAdmin for database management UI:

```bash
# Start PostgreSQL with pgAdmin
docker-compose --profile tools up -d

# Access pgAdmin at http://localhost:5050
# Email: admin@tampavolunteers.local
# Password: admin
```

To stop the services:

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (deletes all data)
docker-compose down -v
```

### Alternative: Manual Database Setup

If you prefer to install PostgreSQL manually:

```sql
CREATE DATABASE tampavolunteers;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE tampavolunteers TO postgres;
```

### 2. Backend Setup

Navigate to the backend directory:

```bash
cd tampavolunteers-backend
```

Update database credentials in `src/main/resources/application.properties` if needed.

Run the application:

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080/api`

### 3. Frontend Setup

Navigate to the frontend directory:

```bash
cd tampavolunteers-frontend
```

Install dependencies:

```bash
npm install
```

Run the development server:

```bash
npm run dev
```

The frontend will start on `http://localhost:5173`

## Environment Variables

### Backend

Create a `.env` file or set environment variables:

- `DB_USERNAME` - PostgreSQL username (default: postgres)
- `DB_PASSWORD` - PostgreSQL password (default: postgres)
- `JWT_SECRET` - Secret key for JWT signing (minimum 256 bits)

### Frontend

Variables are configured in `.env.development` and `.env.production`:

- `VITE_API_BASE_URL` - Backend API URL

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT
- `GET /api/auth/me` - Get current user

### Opportunities (Public)
- `GET /api/opportunities` - List opportunities
- `GET /api/opportunities/{id}` - Get opportunity details

### Categories & Skills (Public)
- `GET /api/categories` - List all categories
- `GET /api/skills` - List all skills

## User Roles

1. **VOLUNTEER** - Can browse opportunities, register, and log hours
2. **ORG_ADMIN** - Can manage own organization and opportunities
3. **ADMIN** - Full platform access

## Default Test Data

After running the database migrations, the following test data will be available:

### Categories
- Environment, Education, Health, Animals, Community, Food & Hunger, Children & Youth, Seniors, Arts & Culture, Sports & Recreation

### Skills
- Teaching, Construction, Medical, Event Planning, Marketing, Fundraising, Graphic Design, Photography, Web Development, Writing, and more

## Development Commands

### Backend

```bash
# Run tests
./mvnw test

# Build JAR
./mvnw clean package

# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend

```bash
# Run dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

## Next Steps

### Backend Tasks
1. Implement OpportunityService and OpportunityController
2. Implement OrganizationService and OrganizationController
3. Implement RegistrationService and RegistrationController
4. Implement VolunteerHoursService and VolunteerHoursController
5. Add unit and integration tests
6. Configure Swagger/OpenAPI documentation

### Frontend Tasks
1. Implement OpportunityService for API calls
2. Create OpportunityList and OpportunityDetail components
3. Implement registration functionality
4. Add volunteer hours tracking
5. Implement organization dashboard
6. Add search and filter functionality
7. Improve UI/UX with loading states and error handling

## Docker Commands

### Database Management

```bash
# View running containers
docker-compose ps

# View database logs
docker-compose logs postgres

# Access PostgreSQL CLI
docker-compose exec postgres psql -U postgres -d tampavolunteers

# Backup database
docker-compose exec postgres pg_dump -U postgres tampavolunteers > backup.sql

# Restore database
docker-compose exec -T postgres psql -U postgres tampavolunteers < backup.sql

# Reset database (removes all data)
docker-compose down -v
docker-compose up -d
```

### Docker Volume Information

The PostgreSQL data is stored in a Docker volume named `tampavolunteers_postgres_data`. This ensures:
- Data persists between container restarts
- Fast database performance
- Easy backup and migration

To inspect the volume:

```bash
# List volumes
docker volume ls

# Inspect volume details
docker volume inspect tampavolunteers_postgres_data
```

**📖 For detailed Docker documentation, see [DOCKER.md](DOCKER.md)**

## Troubleshooting

### Backend won't start
- Ensure PostgreSQL is running:
  - **Docker:** `docker-compose ps` (should show postgres as "Up")
  - **Manual:** Check PostgreSQL service status
- Check database credentials in `application.properties`
- Verify Java 17+ is installed: `java -version`

### Frontend won't start
- Delete `node_modules` and run `npm install` again
- Check Node.js version: `node --version` (should be 18+)
- Verify backend is running on port 8080

### Database connection errors
- **Docker:** Run `docker-compose up -d` to start PostgreSQL
- **Manual:** Verify PostgreSQL service is running
- Check database exists: `psql -l` or `docker-compose exec postgres psql -U postgres -l`
- Verify credentials match in `application.properties`

### Docker issues
- Port 5432 already in use: Stop local PostgreSQL or change port in `docker-compose.yml`
- Permission denied: Ensure Docker daemon is running
- Container won't start: Check logs with `docker-compose logs postgres`

## License

This project is for educational purposes.
