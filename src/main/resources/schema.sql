-- ==============================================================================
-- THE WINTER ARK - COMPLETE POSTGRESQL DDL & MIGRATION SCRIPT
-- Compatible with PostgreSQL 14+, Neon PostgreSQL, AWS RDS, and Supabase.
-- ==============================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==============================================================================
-- SECTION A: COMPLETE SCHEMA DEFINITIONS (CREATE IF NOT EXISTS)
-- ==============================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Goals Table (Includes is_archived, start_date, end_date)
CREATE TABLE IF NOT EXISTS goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    tag_line VARCHAR(255),
    start_date DATE,
    end_date DATE,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Goal Active Days (Collection Table for Scheduled Days of the Week)
CREATE TABLE IF NOT EXISTS goal_active_days (
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    day_of_week VARCHAR(20) NOT NULL,
    PRIMARY KEY (goal_id, day_of_week)
);

-- 4. Predefined Tasks Table (Global Routine Habits)
CREATE TABLE IF NOT EXISTS predefined_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    task_content VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Daily Logs Table (Tracks Day-by-Day Schedules & Rest Days)
CREATE TABLE IF NOT EXISTS daily_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    target_date DATE NOT NULL,
    is_rest_day BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_daily_logs_goal_target_date UNIQUE (goal_id, target_date)
);

-- 6. Daily Tasks Table (Includes Status: PENDING / COMPLETED / SKIPPED)
CREATE TABLE IF NOT EXISTS daily_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_log_id UUID NOT NULL REFERENCES daily_logs(id) ON DELETE CASCADE,
    predefined_task_id UUID REFERENCES predefined_tasks(id) ON DELETE SET NULL,
    task_content VARCHAR(500) NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    is_ad_hoc BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Friendships Table (Social Squad Requests & Mutual Connections)
CREATE TABLE IF NOT EXISTS friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_friendships_sender_receiver UNIQUE (sender_id, receiver_id)
);

-- 8. Goal Shares Table (Granular Squad Permissions & Sharing)
CREATE TABLE IF NOT EXISTS goal_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_goal_shares_goal_friend UNIQUE (goal_id, friend_id)
);

-- 9. Push Subscriptions Table (Web Push Device Registry)
CREATE TABLE IF NOT EXISTS push_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL UNIQUE,
    keys_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- SECTION B: INDEXES FOR PERFORMANCE & FAST RETRIEVAL
-- ==============================================================================

CREATE INDEX IF NOT EXISTS idx_goals_user_archived ON goals(user_id, is_archived);
CREATE INDEX IF NOT EXISTS idx_daily_logs_goal_date ON daily_logs(goal_id, target_date DESC);
CREATE INDEX IF NOT EXISTS idx_daily_tasks_log_status ON daily_tasks(daily_log_id, status);
CREATE INDEX IF NOT EXISTS idx_predefined_tasks_goal ON predefined_tasks(goal_id);
CREATE INDEX IF NOT EXISTS idx_friendships_sender_status ON friendships(sender_id, status);
CREATE INDEX IF NOT EXISTS idx_friendships_receiver_status ON friendships(receiver_id, status);
CREATE INDEX IF NOT EXISTS idx_goal_shares_friend ON goal_shares(friend_id);
CREATE INDEX IF NOT EXISTS idx_goal_shares_goal ON goal_shares(goal_id);
CREATE INDEX IF NOT EXISTS idx_push_subscriptions_user ON push_subscriptions(user_id);

-- ==============================================================================
-- SECTION C: ALTER MIGRATION SCRIPT (FOR EXISTING DATABASES)
-- Run this block if upgrading an already existing database instance.
-- ==============================================================================

-- 1. Add `is_archived` column to `goals` table if not present
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name='goals' AND column_name='is_archived'
    ) THEN
        ALTER TABLE goals ADD COLUMN is_archived BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;

-- 2. Add `status` column to `daily_tasks` table if not present and backfill
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name='daily_tasks' AND column_name='status'
    ) THEN
        ALTER TABLE daily_tasks ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING';
        UPDATE daily_tasks SET status = CASE WHEN is_completed = TRUE THEN 'COMPLETED' ELSE 'PENDING' END;
    END IF;
END $$;

-- 3. Ensure `start_date` and `end_date` columns exist on `goals`
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name='goals' AND column_name='start_date'
    ) THEN
        ALTER TABLE goals ADD COLUMN start_date DATE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name='goals' AND column_name='end_date'
    ) THEN
        ALTER TABLE goals ADD COLUMN end_date DATE;
    END IF;
END $$;

-- 4. Ensure `push_subscriptions` table exists with unique endpoint constraint
CREATE TABLE IF NOT EXISTS push_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL UNIQUE,
    keys_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
