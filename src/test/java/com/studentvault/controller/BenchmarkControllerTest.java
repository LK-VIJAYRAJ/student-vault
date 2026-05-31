package com.studentvault.controller;

import com.studentvault.dto.BenchmarkDTO;
import com.studentvault.service.BenchmarkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BenchmarkController.class)
@DisplayName("BenchmarkController — WebMvcTest Slice Tests")
class BenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BenchmarkService benchmarkService;

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/benchmark/run
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/benchmark/run — returns 200 with benchmark report")
    void runBenchmark_returns200WithReport() throws Exception {
        // Arrange — build a representative mock report matching what the live API returns
        BenchmarkDTO rollNumberBenchmark = BenchmarkDTO.builder()
                .queryDescription("Find student by roll number (idx_students_roll_number)")
                .withoutIndexMs(743L)
                .withIndexMs(3L)
                .improvement("247.7x faster")
                .queryPlan("Use EXPLAIN ANALYZE in psql for full plan details")
                .build();

        BenchmarkDTO compositeBenchmark = BenchmarkDTO.builder()
                .queryDescription("Get results by student + semester (idx_results_student_semester)")
                .withoutIndexMs(612L)
                .withIndexMs(8L)
                .improvement("76.5x faster")
                .queryPlan("Use EXPLAIN ANALYZE in psql for full plan details")
                .build();

        BenchmarkDTO.BenchmarkReport report = BenchmarkDTO.BenchmarkReport.builder()
                .generatedAt("2024-05-25T10:30:00")
                .totalStudents(50_000L)
                .totalResults(200_000L)
                .benchmarks(List.of(rollNumberBenchmark, compositeBenchmark))
                .summary("Ran 2 benchmark queries on 50000 student records. " +
                         "Average improvement with indexes: 85.0x faster.")
                .build();

        given(benchmarkService.runBenchmark()).willReturn(report);

        // Act + Assert
        mockMvc.perform(get("/api/benchmark/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(50_000))
                .andExpect(jsonPath("$.totalResults").value(200_000))
                .andExpect(jsonPath("$.benchmarks").isArray())
                .andExpect(jsonPath("$.benchmarks.length()").value(2))
                .andExpect(jsonPath("$.benchmarks[0].queryDescription")
                        .value("Find student by roll number (idx_students_roll_number)"))
                .andExpect(jsonPath("$.benchmarks[0].withoutIndexMs").value(743))
                .andExpect(jsonPath("$.benchmarks[0].withIndexMs").value(3))
                .andExpect(jsonPath("$.benchmarks[0].improvement").value("247.7x faster"))
                .andExpect(jsonPath("$.summary").isString());

        then(benchmarkService).should().runBenchmark();
    }

    @Test
    @DisplayName("GET /api/benchmark/run — returns 500 when benchmark service throws")
    void runBenchmark_returns500WhenServiceThrows() throws Exception {
        given(benchmarkService.runBenchmark())
                .willThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/api/benchmark/run"))
                .andExpect(status().isInternalServerError());
    }
}
