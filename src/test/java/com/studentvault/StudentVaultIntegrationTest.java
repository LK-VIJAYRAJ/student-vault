package com.studentvault;

import com.studentvault.dto.BenchmarkDTO;
import com.studentvault.dto.StudentDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests using Testcontainers — spins up a real PostgreSQL container,
 * runs Flyway migrations (including seed data), then exercises the full HTTP stack.
 *
 * Interview talking point:
 *   "I use Testcontainers so integration tests run against a real PostgreSQL
 *    engine, not an in-memory database. This catches SQL dialect issues early."
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("StudentVault — End-to-End Integration Tests")
class StudentVaultIntegrationTest {

    // Testcontainers starts a real PostgreSQL 16; @ServiceConnection wires it
    // automatically into Spring's DataSource — no manual config override needed.
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    // Student created in the CREATE test, shared across ordered tests
    private static Long createdStudentId;

    // ─────────────────────────────────────────────────────────────────────────
    //  HEALTH CHECK
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("GET /actuator/health — returns UP")
    void healthEndpoint_returnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STUDENT CRUD FLOW
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("POST /api/students — creates student and returns 201")
    void createStudent_returns201WithBody() {
        StudentDTO.Request request = StudentDTO.Request.builder()
                .name("Integration Tester")
                .email("integration@test.com")
                .rollNumber("IT000001")
                .departmentId(1L) // CSE dept seeded by V2 migration
                .semester(2)
                .phone("9000000001")
                .build();

        ResponseEntity<StudentDTO.Response> response = restTemplate.postForEntity(
                "/api/students", request, StudentDTO.Response.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRollNumber()).isEqualTo("IT000001");
        assertThat(response.getBody().getDepartmentCode()).isEqualTo("CSE");

        createdStudentId = response.getBody().getId();
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/students/{id} — fetches the created student")
    void getStudentById_returnsCreatedStudent() {
        assertThat(createdStudentId).isNotNull();

        ResponseEntity<StudentDTO.Response> response = restTemplate.getForEntity(
                "/api/students/" + createdStudentId, StudentDTO.Response.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Integration Tester");
        assertThat(response.getBody().getEmail()).isEqualTo("integration@test.com");
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/students/roll/{rollNumber} — fetches by roll (tests index path)")
    void getStudentByRollNumber_returnsStudent() {
        ResponseEntity<StudentDTO.Response> response = restTemplate.getForEntity(
                "/api/students/roll/IT000001", StudentDTO.Response.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRollNumber()).isEqualTo("IT000001");
    }

    @Test
    @Order(5)
    @DisplayName("PUT /api/students/{id} — updates student semester")
    void updateStudent_returnsUpdatedStudent() {
        assertThat(createdStudentId).isNotNull();

        StudentDTO.Request updateRequest = StudentDTO.Request.builder()
                .name("Integration Tester")
                .email("integration@test.com")
                .rollNumber("IT000001")
                .departmentId(1L)
                .semester(4) // promoted to semester 4
                .phone("9000000001")
                .build();

        ResponseEntity<StudentDTO.Response> response = restTemplate.exchange(
                "/api/students/" + createdStudentId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                StudentDTO.Response.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSemester()).isEqualTo(4);
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/students — returns 409 on duplicate email")
    void createStudent_returns409OnDuplicateEmail() {
        StudentDTO.Request duplicate = StudentDTO.Request.builder()
                .name("Duplicate User")
                .email("integration@test.com") // same as already created
                .rollNumber("IT000002")
                .departmentId(1L)
                .semester(1)
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/students", duplicate, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/students/{id} — returns 404 for non-existent student")
    void getStudent_returns404ForUnknownId() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/students/9999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/students — returns paginated list with seeded students")
    void getAllStudents_returnsPaginatedList() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/students?page=0&size=10", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content");
        assertThat(response.getBody()).contains("totalElements");
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /api/students/{id} — deletes and returns 204")
    void deleteStudent_returns204() {
        assertThat(createdStudentId).isNotNull();

        restTemplate.delete("/api/students/" + createdStudentId);

        // Verify it's gone
        ResponseEntity<String> checkResponse = restTemplate.getForEntity(
                "/api/students/" + createdStudentId, String.class);
        assertThat(checkResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BENCHMARK ENDPOINT
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("GET /api/benchmark/run — returns valid benchmark report")
    void benchmarkEndpoint_returnsValidReport() {
        ResponseEntity<BenchmarkDTO.BenchmarkReport> response = restTemplate.getForEntity(
                "/api/benchmark/run", BenchmarkDTO.BenchmarkReport.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBenchmarks()).isNotEmpty();
        assertThat(response.getBody().getTotalStudents()).isGreaterThan(0);
        assertThat(response.getBody().getSummary()).contains("faster");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RESULTS ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("GET /api/results/toppers — returns topper list for department 1")
    void getToppers_returnsTopperList() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/results/toppers?departmentId=1&semester=3&limit=5", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/results/grade-distribution — returns grade distribution for semester 4")
    void getGradeDistribution_returnsDistribution() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/results/grade-distribution?semester=4", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
