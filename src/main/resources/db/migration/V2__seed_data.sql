-- V2: Seed realistic data (departments, courses, then bulk students + results)

-- Departments
INSERT INTO departments (name, code) VALUES
    ('Computer Science Engineering',     'CSE'),
    ('Electronics & Communication Eng.', 'ECE'),
    ('Mechanical Engineering',           'MECH'),
    ('Civil Engineering',                'CIVIL'),
    ('Information Technology',           'IT'),
    ('Electrical Engineering',           'EE')
ON CONFLICT DO NOTHING;

-- Courses
INSERT INTO courses (code, name, department_id, credits) VALUES
    ('CSE101', 'Introduction to Programming',        1, 4),
    ('CSE201', 'Data Structures & Algorithms',       1, 4),
    ('CSE301', 'Database Management Systems',        1, 4),
    ('CSE401', 'Operating Systems',                  1, 4),
    ('CSE501', 'Computer Networks',                  1, 3),
    ('CSE601', 'Software Engineering',               1, 3),
    ('CSE701', 'Machine Learning',                   1, 4),
    ('CSE801', 'Cloud Computing',                    1, 3),
    ('ECE101', 'Basic Electronics',                  2, 4),
    ('ECE201', 'Digital Signal Processing',          2, 4),
    ('ECE301', 'Microprocessors',                    2, 4),
    ('IT101',  'Web Technologies',                   5, 3),
    ('IT201',  'Mobile Application Development',     5, 3),
    ('IT301',  'Information Security',               5, 4),
    ('MECH101','Engineering Mechanics',              3, 4),
    ('MECH201','Thermodynamics',                     3, 4),
    ('CIVIL101','Structural Analysis',               4, 4),
    ('EE101',  'Circuit Theory',                     6, 4),
    ('MATH101', 'Engineering Mathematics I',         1, 4),
    ('MATH201', 'Engineering Mathematics II',        1, 4)
ON CONFLICT DO NOTHING;

-- Generate 50,000 students using generate_series
INSERT INTO students (name, email, roll_number, department_id, semester, date_of_birth, phone)
SELECT
    'Student_' || gs                                   AS name,
    'student_' || gs || '@university.edu'              AS email,
    CASE (gs % 6)
        WHEN 0 THEN 'CSE'
        WHEN 1 THEN 'ECE'
        WHEN 2 THEN 'MECH'
        WHEN 3 THEN 'CIVIL'
        WHEN 4 THEN 'IT'
        ELSE 'EE'
    END || LPAD(gs::TEXT, 6, '0')                      AS roll_number,
    (gs % 6) + 1                                       AS department_id,
    (gs % 8) + 1                                       AS semester,
    DATE '2000-01-01' + (gs % 1460) * INTERVAL '1 day' AS date_of_birth,
    '98' || LPAD((gs % 100000000)::TEXT, 8, '0')       AS phone
FROM generate_series(1, 50000) AS gs
ON CONFLICT DO NOTHING;

-- Generate results: each student gets results for 4 courses in their active semester
-- This creates roughly 200,000 result records
INSERT INTO results (student_id, course_id, semester, marks, grade, exam_date)
SELECT
    s.id                                                          AS student_id,
    ((s.id + course_offset - 1) % 20) + 1                       AS course_id,
    s.semester,
    ROUND((RANDOM() * 60 + 35)::NUMERIC, 2)                     AS marks,
    CASE
        WHEN RANDOM() > 0.9  THEN 'O'
        WHEN RANDOM() > 0.7  THEN 'A+'
        WHEN RANDOM() > 0.5  THEN 'A'
        WHEN RANDOM() > 0.3  THEN 'B+'
        WHEN RANDOM() > 0.15 THEN 'B'
        ELSE 'C'
    END                                                           AS grade,
    DATE '2024-01-01' + (s.semester * 30) * INTERVAL '1 day'    AS exam_date
FROM students s
CROSS JOIN generate_series(1, 4) AS course_offset
ON CONFLICT DO NOTHING;
