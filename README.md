# StudentVault 🎓

![CI](https://github.com/LK-VIJAYRAJ/student-vault/actions/workflows/ci.yml/badge.svg)

A production-grade **Student Result Management System** built with **Spring Boot + PostgreSQL**, featuring real database query optimization with measurable benchmarks.

> **Interview Story**: *"I seeded 50,000 students and 200,000 results into PostgreSQL, identified full table scans, added targeted B-tree and composite indexes, and demonstrated up to 21x query improvement on the composite index path — measured live, via an API endpoint."*

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| Observability | Spring Boot Actuator |
| Build | Maven |
| Deployment | Docker + Docker Compose |
| CI | GitHub Actions |

---

## 🏃 Run Locally (2 commands)

**Prerequisites:** Docker Desktop installed and running.

```bash
# Clone and run
git clone https://github.com/LK-VIJAYRAJ/student-vault.git
cd student-vault
docker-compose up --build
```

App starts at: `http://localhost:8080`

> ⏳ First run seeds 50,000 students + 200,000 results via Flyway. This takes ~60 seconds.

---

## 🔑 The Standout Feature: Live Benchmark

Hit this endpoint to see **real before/after query improvement numbers**:

```
GET http://localhost:8080/api/benchmark/run
```

**Sample Response:**
```json
{
  "generatedAt": "2026-05-31T01:25:49",
  "totalStudents": 50000,
  "totalResults": 200000,
  "benchmarks": [
    {
      "queryDescription": "Find student by roll number (idx_students_roll_number)",
      "withoutIndexMs": 5,
      "withIndexMs": 2,
      "improvement": "2.5x faster"
    },
    {
      "queryDescription": "Get results by student + semester (idx_results_student_semester)",
      "withoutIndexMs": 21,
      "withIndexMs": 1,
      "improvement": "21.0x faster"
    },
    {
      "queryDescription": "Find top 10 students in a department (JOIN + ORDER BY marks)",
      "withoutIndexMs": 18,
      "withIndexMs": 3,
      "improvement": "6.0x faster"
    }
  ],
  "summary": "Ran 5 benchmark queries on 50000 student records. Average improvement with indexes: 7.0x faster."
}
```

> 💡 Numbers reflect real PostgreSQL 16 with 50K rows on Docker Desktop (WSL2).
> On a cold production database the gap is larger — run `EXPLAIN ANALYZE` below to see the full query plan.

---

## 📡 API Endpoints

### Students
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/students?page=0&size=20` | Paginated student list |
| GET | `/api/students/{id}` | Student by ID |
| GET | `/api/students/roll/{rollNumber}` | Student by roll number |
| GET | `/api/students/search?name=&departmentId=&semester=` | Search with filters |
| GET | `/api/students/by-dept-semester?departmentId=1&semester=4` | Filter by dept + semester |
| POST | `/api/students` | Create student |
| PUT | `/api/students/{id}` | Update student |
| DELETE | `/api/students/{id}` | Delete student |

### Results
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/results/student/{studentId}` | All results for a student |
| GET | `/api/results/student/{id}/semester/{sem}` | Results by student + semester |
| GET | `/api/results/toppers?departmentId=1&semester=3&limit=10` | Top performers |
| GET | `/api/results/averages?departmentId=1` | Average marks per course |
| GET | `/api/results/grade-distribution?semester=4` | Grade distribution |
| POST | `/api/results` | Add a result |

### Benchmark & Observability
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/benchmark/run` | **Live query optimization benchmark** |
| GET | `/actuator/health` | Application health |
| GET | `/actuator/metrics` | Application metrics |

---

## 🗄️ Database Schema

```
departments (id, name, code)
    ↓ 1:N
students (id, name, email, roll_number, department_id, semester)
    ↓ 1:N
results (id, student_id, course_id, semester, marks, grade)
    ↑ N:1
courses (id, code, name, department_id, credits)
```

---

## ⚡ Query Optimization — The Core Story

### Indexes Added (V3 Migration)

```sql
-- Single column indexes
idx_students_roll_number      → exact roll number lookups
idx_students_department_id    → filter by department
idx_students_semester         → filter by semester

-- Composite index (the interesting one)
idx_students_dept_semester    → WHERE department_id=? AND semester=?
                               Column order: higher-cardinality first

-- Results table
idx_results_student_id        → JOIN on student
idx_results_student_semester  → WHERE student_id=? AND semester=?
idx_results_semester_marks    → ORDER BY marks DESC (topper query)
```

### Why These Indexes?
- **B-tree index** on `roll_number`: transforms `O(N)` sequential scan → `O(log N)` index scan
- **Composite index** `(student_id, semester)`: eliminates need to filter in memory after index scan; left-prefix rule means it also covers `WHERE student_id=?` alone
- **Index on marks**: supports `ORDER BY marks DESC` without sort step

### Run EXPLAIN ANALYZE Yourself
```sql
-- Connect to PostgreSQL
docker exec -it studentvault-db psql -U postgres -d studentvault

-- See full query plan
EXPLAIN ANALYZE SELECT * FROM students WHERE roll_number = 'CSE000042';
EXPLAIN ANALYZE SELECT * FROM results WHERE student_id = 1000 AND semester = 4;
```

---

## 🧱 Project Structure

```
src/main/java/com/studentvault/
├── StudentVaultApplication.java
├── controller/
│   ├── StudentController.java
│   ├── ResultController.java
│   └── BenchmarkController.java
├── service/
│   ├── StudentService.java
│   ├── ResultService.java
│   └── BenchmarkService.java       ← The standout feature
├── repository/
│   ├── StudentRepository.java
│   ├── ResultRepository.java
│   ├── CourseRepository.java
│   └── DepartmentRepository.java
├── model/
│   ├── Student.java, Result.java, Course.java, Department.java
├── dto/
│   ├── StudentDTO.java, ResultDTO.java, BenchmarkDTO.java
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    └── DuplicateResourceException.java

src/main/resources/db/migration/
├── V1__create_schema.sql     ← Tables
├── V2__seed_data.sql         ← 50K students + 200K results
└── V3__add_indexes.sql       ← The optimization step
```

---

## 💬 Interview Talking Points

**Q: Why PostgreSQL over MySQL?**
> PostgreSQL's `EXPLAIN ANALYZE` gives actual execution times, not just estimates. Also supports composite indexes with column ordering, which was critical for the `(student_id, semester)` compound query.

**Q: Why did you choose those specific indexes?**
> I ran `EXPLAIN ANALYZE` first to identify full sequential scans. Then added indexes only where the query selectivity justified the write overhead. For example, `roll_number` has very high cardinality — perfect for a B-tree index.

**Q: What's the tradeoff of adding indexes?**
> Indexes speed up reads but slow down writes (INSERT/UPDATE/DELETE must update the index). Also consume disk space. I chose not to index low-cardinality columns like `grade` because the query planner wouldn't use them effectively.

**Q: How did you seed 50,000 records?**
> Using PostgreSQL's `generate_series()` function in a Flyway migration. No application code needed — pure SQL. Runs once on first startup.

---

## 🚢 Deploy to Railway (Free)

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login and deploy
railway login
railway init
railway up
railway add postgresql
```

Set environment variables in Railway dashboard matching `application-docker.yml`.

---

## 👤 Author
Built as a portfolio project demonstrating real database optimization for SDE interviews.
