# Password Reset Guide

This guide provides multiple methods to reset user passwords and update email addresses in the Tampa Volunteers application.

## Method 1: Interactive Java Utility (Recommended)

This method uses a built-in Spring Boot utility with BCrypt password hashing.

### Using the Shell Script

**On macOS/Linux:**
```bash
./reset-password.sh
```

**On Windows:**
```cmd
reset-password.bat
```

### Manual Execution

**With Maven:**
```bash
cd tampavolunteers-backend
./mvnw spring-boot:run -Dspring-boot.run.arguments="--reset-password=true"
```

**With Docker (if backend is built):**
```bash
docker-compose run --rm backend java -jar app.jar --reset-password=true
```

### Features

The utility provides an interactive menu with these options:

1. **List all users** - View all registered users with their details
2. **Reset password by email** - Change a user's password
3. **Update email address** - Change a user's email (username)
4. **Exit** - Close the utility

---

## Method 2: Direct Database Access (Quick)

For quick password resets or bulk operations, you can connect directly to PostgreSQL.

### Connect to Database

```bash
# Using Docker
docker exec -it tampavolunteers-db psql -U postgres -d tampavolunteers

# Or if PostgreSQL is running locally
psql -U postgres -d tampavolunteers
```

### List All Users

```sql
SELECT id, email, first_name, last_name, role, auth_provider
FROM users
ORDER BY id;
```

### Update Email

```sql
-- Update user's email
UPDATE users
SET email = 'newemail@example.com'
WHERE email = 'oldemail@example.com';

-- Or by user ID
UPDATE users
SET email = 'newemail@example.com'
WHERE id = 1;
```

### Reset Password (Requires BCrypt Hash)

For passwords, you MUST use a BCrypt hash. Use the Java utility (Method 1) or generate a hash:

**Generate BCrypt Hash Online:**
- Visit: https://bcrypt-generator.com/
- Set rounds to **12** (to match the application's security config)
- Enter your desired password
- Copy the generated hash

**Update with BCrypt Hash:**
```sql
UPDATE users
SET password_hash = '$2a$12$YourGeneratedBCryptHashHere'
WHERE email = 'user@example.com';
```

**Example:**
```sql
-- Reset password to "MyNewPassword123"
-- (hash generated with BCrypt rounds=12)
UPDATE users
SET password_hash = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lW7qTp.HZW7u'
WHERE email = 'john@example.com';
```

---

## Method 3: Using pgAdmin (GUI)

pgAdmin provides a graphical interface for database management.

### Start pgAdmin

```bash
docker-compose --profile tools up pgadmin
```

### Access pgAdmin

1. Open browser: http://localhost:5050
2. Login credentials:
   - Email: `admin@tampavolunteers.local`
   - Password: `admin`

3. Add server connection:
   - Name: `Tampa Volunteers`
   - Host: `postgres` (or `localhost` if not using Docker)
   - Port: `5432`
   - Database: `tampavolunteers`
   - Username: `postgres`
   - Password: `postgres`

4. Navigate to: Servers → Tampa Volunteers → Databases → tampavolunteers → Schemas → public → Tables → users

5. Right-click on `users` → View/Edit Data → All Rows

6. Edit the email or password_hash directly (remember: password_hash must be BCrypt!)

---

## Quick Reference

### Default Database Credentials
- Host: `localhost:5432`
- Database: `tampavolunteers`
- Username: `postgres`
- Password: `postgres`

### Password Requirements
- Minimum 8 characters
- Must be BCrypt hashed with 12 rounds when updating directly in database

### User Roles
- `VOLUNTEER` - Regular volunteer user
- `ORG_ADMIN` - Organization administrator
- `ADMIN` - System administrator

### Authentication Providers
- `LOCAL` - Email/password authentication
- `GOOGLE` - Google OAuth
- `GITHUB` - GitHub OAuth

---

## Troubleshooting

### "User not found"
- Verify the email address is correct
- Use Method 2 to list all users and check spelling

### "Email already in use"
- The new email address is already registered
- Choose a different email or delete the conflicting account

### Password doesn't work after reset
- Ensure you used BCrypt with 12 rounds
- Try using the Java utility (Method 1) instead of manual SQL
- Check that you're logging in with the correct email

### Can't connect to database
```bash
# Check if database is running
docker ps | grep tampavolunteers-db

# Start database if needed
docker-compose up -d postgres

# Check database logs
docker logs tampavolunteers-db
```

---

## Security Notes

⚠️ **Important Security Considerations:**

1. **Never** commit password reset scripts with hardcoded credentials
2. **Always** use BCrypt for password hashing (the Java utility does this automatically)
3. **Never** store passwords in plain text
4. **Rotate** database credentials in production
5. **Limit** access to the password reset utility in production environments
6. **Log** all password reset operations for security audits

---

## Examples

### Example 1: Reset Admin Password
```bash
./reset-password.sh
# Select option 2
# Enter email: admin@tampavolunteers.com
# Enter new password: SecurePassword123!
# Confirm password: SecurePassword123!
```

### Example 2: Change User Email
```bash
./reset-password.sh
# Select option 3
# Enter current email: old@example.com
# Enter new email: new@example.com
```

### Example 3: View All Users
```bash
./reset-password.sh
# Select option 1
# Review the list of users
```

---

For additional help, refer to the main [README.md](README.md) or contact the development team.
