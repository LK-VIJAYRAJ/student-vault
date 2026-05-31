-- V1: Create base schema
-- Departments
CREATE TABLE IF NOT EXISTS departments (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Students
CREATE TABLE IF NOT EXISTS students (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    roll_number   VARCHAR(30)  NOT NULL UNIQUE,
    department_id BIGINT       NOT NULL REFERENCES departments (id),
    semester      INT          NOT NULL CHECK (semester BETWEEN 1 AND 8),
    date_of_birth DATE,
    phone         VARCHAR(15),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Courses
CREATE TABLE IF NOT EXISTS courses (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL UNIQUE,
    name          VARCHAR(150) NOT NULL,
    department_id BIGINT       NOT NULL REFERENCES departments (id),
    credits       INT          NOT NULL CHECK (credits BETWEEN 1 AND 6),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Results
CREATE TABLE IF NOT EXISTS results (
    id         BIGSERIAL PRIMARY KEY,
    student_id BIGINT         NOT NULL REFERENCES students (id),
    course_id  BIGINT         NOT NULL REFERENCES courses (id),
    semester   INT            NOT NULL CHECK (semester BETWEEN 1 AND 8),
    marks      DECIMAL(5, 2)  NOT NULL CHECK (marks BETWEEN 0 AND 100),
    grade      VARCHAR(5)     NOT NULL,
    exam_date  DATE           NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, course_id, semester)
);
