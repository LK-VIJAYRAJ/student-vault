# StudentVault 🎓

![CI](https://github.com/LK-VIJAYRAJ/student-vault/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Swagger](https://img.shields.io/badge/Swagger-UI-85EA2D?logo=swagger)

A production-grade **Student Result Management System** built with **Spring Boot + PostgreSQL**, featuring real database query optimization with measurable benchmarks.

> **Interview Story**: *"I seeded 50,000 students and 200,000 results into PostgreSQL, identified full table scans, added targeted B-tree and composite indexes, and demonstrated up to 21x query improvement on the composite index path — measured live, via an API endpoint."*

---

## 🌐 Live Demo

| Resource | URL |
|---|---|
| **Swagger UI** | https://student-vault-584m.onrender.com/swagger-ui.html |
| **Benchmark endpoint** | https://student-vault-584m.onrender.com/api/benchmark/run |
| **Health check** | https://student-vault-584m.onrender.com/actuator/health |

> ⏳ Free tier spins down after inactivity — first request may take ~50s to wake up. After that, all endpoints respond instantly.

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Observability | Spring Boot Actuator |
| Build | Maven |
| Deployment | Docker + Docker Compose + Railway |
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

| URL | What it is |
|---|---|
| `http://localhost:8080/swagger-ui.html` | 📖 Interactive API documentation |
| `http://localhost:8080/api/benchmark/run` | ⚡ Live benchmark (the main demo) |
| `http://localhost:8080/actuator/health` | 💚 Health check |

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

Full interactive documentation: **[/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

### Students
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/students?page=0&size=20` | Paginated student list |
| GET | `/api/students/{id}` | Student by ID |
| GET | `/api/students/roll/{rollNumber}` | Student by roll number (**B-tree index**) |
| GET | `/api/students/search?name=&departmentId=&semester=` | Search with filters |
| GET | `/api/students/by-dept-semester?departmentId=1&semester=4` | Filter by dept + semester (**composite index**) |
| POST | `/api/students` | Create student |
| PUT | `/api/students/{id}` | Update student |
| DELETE | `/api/students/{id}` | Delete student |

### Results
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/results/student/{studentId}` | All results for a student |
| GET | `/api/results/student/{id}/semester/{sem}` | Results by student + semester (**21x speedup**) |
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

## 🧪 Testing

### Unit Tests (no Docker required)
```bash
mvn test
```
Runs: `StudentServiceTest`, `ResultServiceTest`, `StudentControllerTest`, `ResultControllerTest`, `BenchmarkControllerTest`
- Uses **Mockito** for service-layer tests
- Uses **`@WebMvcTest`** slice for controller tests (no full Spring context)
- Zero external dependencies — works on any machine

### Integration Tests (Docker required)
```bash
# Requires Docker Desktop running
mvn test -P integration-tests
```
Runs: `StudentVaultIntegrationTest`
- Uses **Testcontainers** to spin up a real PostgreSQL 16 container
- Runs all Flyway migrations (including 50K seed data)
- Tests the full HTTP stack end-to-end

> **Windows note**: Integration tests use `NpipeSocketClientProviderStrategy` (configured in `testcontainers.properties`) which requires Docker Desktop with WSL2 engine. If you see a `DockerClientException`, ensure Docker Desktop is running and WSL2 integration is enabled in Docker Desktop settings.

### CI Pipeline
GitHub Actions runs **unit tests only** on every push to `main`/`develop` — no Docker needed in CI.

---

## 🧱 Project Structure

```
src/main/java/com/studentvault/
├── StudentVaultApplication.java
├── config/
│   └── OpenApiConfig.java               ← Swagger/OpenAPI metadata
├── controller/
│   ├── StudentController.java           ← @Tag + @Operation documented
│   ├── ResultController.java            ← @Tag + @Operation documented
│   └── BenchmarkController.java         ← The standout feature
├── service/
│   ├── StudentService.java
│   ├── ResultService.java
│   └── BenchmarkService.java
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

src/main/resources/
├── application.yml                      ← Local dev config
├── application-docker.yml               ← Docker Compose profile
├── application-railway.yml              ← Railway production profile
└── db/migration/
    ├── V1__create_schema.sql            ← Tables
    ├── V2__seed_data.sql                ← 50K students + 200K results
    └── V3__add_indexes.sql              ← The optimization step
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

**Q: How are the unit and integration tests structured?**
> Unit tests use Mockito and `@WebMvcTest` — zero external dependencies, run in CI. Integration tests use Testcontainers to spin up a real PostgreSQL container, then run all Flyway migrations and test the full HTTP stack. I separated them via a Maven profile (`-P integration-tests`) so CI stays fast.

**Q: How is it deployed?**
> Dockerised with a multi-stage build (Maven build layer → Alpine JRE runtime). Deployed to Railway via `railway.toml` config-as-code. The app uses Spring profiles (`-Dspring.profiles.active=railway`) to pick up Railway-injected environment variables for the database connection.

---

## 🚀 Deploy to Koyeb (Recommended — Always-On, No Cold Starts)

Koyeb keeps your app running 24/7 on its free tier — no 10-second cold start delays.

### Prerequisites
- [Koyeb account](https://www.koyeb.com) — free, no credit card
- [Neon.tech account](https://neon.tech) — free serverless PostgreSQL
- GitHub repo (`LK-VIJAYRAJ/student-vault`)

### Step 1 — Create a Neon PostgreSQL Database
1. Sign up at https://neon.tech
2. Create a new project → select region closest to you
3. Copy the **connection string** — it looks like:
   ```
   postgres://user:password@ep-xxx.region.aws.neon.tech/neondb?sslmode=require
   ```
4. Split it into these parts (you'll need them in Step 3):
   | Variable | Value |
   |---|---|
   | `PGHOST` | `ep-xxx.region.aws.neon.tech` |
   | `PGPORT` | `5432` |
   | `PGDATABASE` | `neondb` |
   | `PGUSER` | `user` |
   | `PGPASSWORD` | `password` |

### Step 2 — Create Koyeb Web Service
1. Sign up at https://www.koyeb.com
2. Click **Create Service** → choose **GitHub**
3. Connect your GitHub account and select repo `LK-VIJAYRAJ/student-vault`
4. Set **Branch** to `main`
5. Set **Builder** to **Dockerfile**
6. Set **Port** to `8080`
7. Set **Health check path** to `/actuator/health`

### Step 3 — Set Environment Variables
In the Koyeb Service settings → **Environment variables**, add:

```
SPRING_PROFILES_ACTIVE = koyeb
PGHOST                 = ep-xxx.region.aws.neon.tech
PGPORT                 = 5432
PGDATABASE             = neondb
PGUSER                 = <your-neon-user>
PGPASSWORD             = <your-neon-password>
APP_BASE_URL           = https://<your-koyeb-app-name>.koyeb.app
```

### Step 4 — Deploy
1. Click **Deploy**
2. First deploy takes ~4–5 minutes (Maven build + Flyway seeding 50K rows)
3. Once health check passes, your app is live at: `https://<your-koyeb-app-name>.koyeb.app`

> ⏳ Neon free tier: The database itself may cold-start after 5 minutes of inactivity,
> but the Spring Boot app stays alive on Koyeb. The first query after DB sleep takes ~1s.

### Step 5 — Update README
Replace the placeholder URLs in the **Live Demo** section above with your Koyeb app URL.

---

## 🚢 Deploy to Railway

### Prerequisites
- [Railway account](https://railway.app) (free tier works)
- GitHub repo connected to Railway

### Steps

```bash
# 1. Install Railway CLI
npm install -g @railway/cli

# 2. Login
railway login

# 3. Create new project from this repo
railway init

# 4. Add PostgreSQL database plugin
railway add --plugin postgresql

# 5. Set environment variables in Railway dashboard:
#    SPRING_PROFILES_ACTIVE = railway
#    (Railway auto-injects JDBC_DATABASE_URL, PGUSER, PGPASSWORD, PORT)

# 6. Deploy
railway up
```

> ⏳ First deploy takes ~3–4 minutes (Maven build + Flyway seeding 50K rows). Subsequent deploys are faster due to Docker layer caching.

After deploy, your app will be live at: `https://<project-name>.up.railway.app`

Update the server URL in `OpenApiConfig.java` to match your Railway domain.

---

## 👤 Author
Built as a portfolio project demonstrating real database optimization for SDE interviews.
