-- Tampa Volunteers Initial Schema

-- Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Organizations Table
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    website VARCHAR(255),
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(20),
    street VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(2),
    zip VARCHAR(10),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_organizations_user_id ON organizations(user_id);
CREATE INDEX idx_organizations_verified ON organizations(verified);

-- Categories Table
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

-- Skills Table
CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Opportunities Table
CREATE TABLE opportunities (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    street VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(2),
    zip VARCHAR(10),
    slots_available INTEGER NOT NULL,
    slots_filled INTEGER NOT NULL DEFAULT 0,
    category_id BIGINT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_opportunities_organization_id ON opportunities(organization_id);
CREATE INDEX idx_opportunities_start_date_time ON opportunities(start_date_time);
CREATE INDEX idx_opportunities_category_id ON opportunities(category_id);
CREATE INDEX idx_opportunities_status ON opportunities(status);

-- Opportunity Skills (Many-to-Many)
CREATE TABLE opportunity_skills (
    opportunity_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (opportunity_id, skill_id),
    FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

-- Registrations Table
CREATE TABLE registrations (
    id BIGSERIAL PRIMARY KEY,
    opportunity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attendance_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (opportunity_id, user_id)
);

CREATE INDEX idx_registrations_opportunity_id ON registrations(opportunity_id);
CREATE INDEX idx_registrations_user_id ON registrations(user_id);
CREATE INDEX idx_registrations_status ON registrations(status);

-- Volunteer Hours Table
CREATE TABLE volunteer_hours (
    id BIGSERIAL PRIMARY KEY,
    registration_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    opportunity_id BIGINT NOT NULL,
    hours_logged DECIMAL(5, 2) NOT NULL,
    verified_by BIGINT,
    notes TEXT,
    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (registration_id) REFERENCES registrations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_volunteer_hours_user_id ON volunteer_hours(user_id);
CREATE INDEX idx_volunteer_hours_opportunity_id ON volunteer_hours(opportunity_id);
CREATE INDEX idx_volunteer_hours_registration_id ON volunteer_hours(registration_id);
