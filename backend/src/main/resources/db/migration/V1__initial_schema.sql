-- MySQL Migration Script
-- Run this manually on your Aiven MySQL database before starting the application

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    google_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    picture TEXT,
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP NULL,
    gmail_connected BOOLEAN DEFAULT FALSE,
    last_sync_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_google_id ON users(google_id);
CREATE INDEX idx_users_email ON users(email);

-- Gmail Accounts table
CREATE TABLE IF NOT EXISTS gmail_accounts (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    gmail_address VARCHAR(255) NOT NULL,
    history_id VARCHAR(255),
    is_primary BOOLEAN DEFAULT TRUE,
    sync_enabled BOOLEAN DEFAULT TRUE,
    last_history_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_gmail_accounts_user_id ON gmail_accounts(user_id);

-- Emails table
CREATE TABLE IF NOT EXISTS emails (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    gmail_account_id CHAR(36) NOT NULL,
    gmail_message_id VARCHAR(255) NOT NULL,
    thread_id VARCHAR(255) NOT NULL,
    sender VARCHAR(500),
    sender_email VARCHAR(500),
    recipient_emails JSON,
    cc_emails JSON,
    bcc_emails JSON,
    subject VARCHAR(1000),
    snippet TEXT,
    body_text LONGTEXT,
    body_html LONGTEXT,
    received_at TIMESTAMP NOT NULL,
    labels JSON,
    has_attachments BOOLEAN DEFAULT FALSE,
    attachment_metadata JSON,
    is_processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (gmail_account_id) REFERENCES gmail_accounts(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_emails_gmail_message_id ON emails(gmail_message_id);
CREATE INDEX idx_emails_gmail_account_id ON emails(gmail_account_id);
CREATE INDEX idx_emails_thread_id ON emails(thread_id);
CREATE INDEX idx_emails_received_at ON emails(received_at DESC);
CREATE INDEX idx_emails_sender_email ON emails(sender_email);
CREATE INDEX idx_emails_is_processed ON emails(is_processed);

-- Email Classifications table
CREATE TABLE IF NOT EXISTS email_classifications (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    email_id CHAR(36) NOT NULL,
    category VARCHAR(50) NOT NULL,
    importance_score INTEGER NOT NULL DEFAULT 0,
    summary TEXT,
    action_required BOOLEAN DEFAULT FALSE,
    action TEXT,
    deadline TIMESTAMP NULL,
    confidence DECIMAL(3,2) DEFAULT 0.0,
    entities JSON,
    model_used VARCHAR(100),
    processing_time_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (email_id) REFERENCES emails(id) ON DELETE CASCADE
);

CREATE INDEX idx_email_classifications_email_id ON email_classifications(email_id);
CREATE INDEX idx_email_classifications_category ON email_classifications(category);
CREATE INDEX idx_email_classifications_importance ON email_classifications(importance_score DESC);

-- Job Applications table
CREATE TABLE IF NOT EXISTS job_applications (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    company VARCHAR(255) NOT NULL,
    role VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'APPLIED',
    applied_at TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    interview_date TIMESTAMP NULL,
    recruiter_name VARCHAR(255),
    recruiter_email VARCHAR(255),
    source_email_ids JSON,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_job_applications_user_id ON job_applications(user_id);
CREATE INDEX idx_job_applications_status ON job_applications(status);
CREATE INDEX idx_job_applications_company ON job_applications(company);

-- Travel Trips table
CREATE TABLE IF NOT EXISTS travel_trips (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    name VARCHAR(255),
    destination VARCHAR(255),
    start_date DATE,
    end_date DATE,
    total_cost DECIMAL(12,2),
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(50) DEFAULT 'PLANNED',
    source_email_ids JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_travel_trips_user_id ON travel_trips(user_id);
CREATE INDEX idx_travel_trips_dates ON travel_trips(start_date, end_date);

-- Flights table
CREATE TABLE IF NOT EXISTS flights (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    trip_id CHAR(36) NOT NULL,
    airline VARCHAR(255),
    flight_number VARCHAR(50),
    departure_airport VARCHAR(10),
    arrival_airport VARCHAR(10),
    departure_city VARCHAR(255),
    arrival_city VARCHAR(255),
    departure_time TIMESTAMP NULL,
    arrival_time TIMESTAMP NULL,
    booking_number VARCHAR(100),
    booking_class VARCHAR(50),
    cost DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'USD',
    source_email_id CHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trip_id) REFERENCES travel_trips(id) ON DELETE CASCADE,
    FOREIGN KEY (source_email_id) REFERENCES emails(id) ON DELETE SET NULL
);

CREATE INDEX idx_flights_trip_id ON flights(trip_id);

-- Hotels table
CREATE TABLE IF NOT EXISTS hotels (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    trip_id CHAR(36) NOT NULL,
    name VARCHAR(255),
    address TEXT,
    city VARCHAR(255),
    country VARCHAR(255),
    check_in_date DATE,
    check_out_date DATE,
    booking_number VARCHAR(100),
    cost DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'USD',
    source_email_id CHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trip_id) REFERENCES travel_trips(id) ON DELETE CASCADE,
    FOREIGN KEY (source_email_id) REFERENCES emails(id) ON DELETE SET NULL
);

CREATE INDEX idx_hotels_trip_id ON hotels(trip_id);

-- Events table
CREATE TABLE IF NOT EXISTS events (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    trip_id CHAR(36),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    location VARCHAR(500),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    is_all_day BOOLEAN DEFAULT FALSE,
    source_email_id CHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (trip_id) REFERENCES travel_trips(id) ON DELETE SET NULL,
    FOREIGN KEY (source_email_id) REFERENCES emails(id) ON DELETE SET NULL
);

CREATE INDEX idx_events_user_id ON events(user_id);
CREATE INDEX idx_events_start_time ON events(start_time);
CREATE INDEX idx_events_type ON events(type);

-- Sync Logs table
CREATE TABLE IF NOT EXISTS sync_logs (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    gmail_account_id CHAR(36) NOT NULL,
    sync_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    emails_fetched INTEGER DEFAULT 0,
    emails_processed INTEGER DEFAULT 0,
    emails_new INTEGER DEFAULT 0,
    emails_updated INTEGER DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    duration_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (gmail_account_id) REFERENCES gmail_accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_sync_logs_gmail_account_id ON sync_logs(gmail_account_id);
CREATE INDEX idx_sync_logs_started_at ON sync_logs(started_at DESC);

-- Flyway schema history table (Flyway will create this automatically)