package com.studentvault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentvault.dto.ResultDTO;
import com.studentvault.exception.DuplicateResourceException;
import com.studentvault.exception.ResourceNotFoundException;
import com.studentvault.service.ResultService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResultController.class)
@DisplayName("ResultController — WebMvcTest Slice Tests")
class ResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ResultService resultService;

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/results/student/{studentId}
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/results/student/{id} — returns 200 with result list")
    void getResultsByStudent_returns200() throws Exception {
        ResultDTO.Response response = ResultDTO.Response.builder()
                .id(1L)
                .studentId(10L)
                .studentName("Bob Kumar")
                .rollNumber("CSE000010")
                .courseName("Data Structures")
                .courseCode("CS301")
                .semester(4)
                .marks(new BigDecimal("87.50"))
                .grade("A")
                .examDate(LocalDate.of(2024, 5, 20))
                .build();

        given(resultService.getResultsByStudent(10L)).willReturn(List.of(response));

        mockMvc.perform(get("/api/results/student/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Bob Kumar"))
                .andExpect(jsonPath("$[0].courseCode").value("CS301"))
                .andExpect(jsonPath("$[0].grade").value("A"))
                .andExpect(jsonPath("$[0].semester").value(4));
    }

    @Test
    @DisplayName("GET /api/results/student/{id} — returns 404 when student not found")
    void getResultsByStudent_returns404WhenNotFound() throws Exception {
        given(resultService.getResultsByStudent(999L))
                .willThrow(new ResourceNotFoundException("Student not found with id: 999"));

        mockMvc.perform(get("/api/results/student/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found with id: 999"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/results/student/{id}/semester/{sem}   ← composite index path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/results/student/{id}/semester/{sem} — returns 200 (composite-index endpoint)")
    void getResultsByStudentAndSemester_returns200() throws Exception {
        ResultDTO.Response response = ResultDTO.Response.builder()
                .id(1L).studentId(10L).semester(4).grade("A")
                .marks(new BigDecimal("87.50"))
                .build();

        given(resultService.getResultsByStudentAndSemester(10L, 4)).willReturn(List.of(response));

        mockMvc.perform(get("/api/results/student/10/semester/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].semester").value(4))
                .andExpect(jsonPath("$[0].grade").value("A"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/results/toppers
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/results/toppers — returns 200 with ranked toppers list")
    void getToppers_returns200() throws Exception {
        ResultDTO.TopperResponse topper = ResultDTO.TopperResponse.builder()
                .studentName("Alice Johnson")
                .rollNumber("CSE000001")
                .courseName("Data Structures")
                .marks(new BigDecimal("98.00"))
                .grade("S")
                .rank(1)
                .build();

        given(resultService.getToppers(1L, 3, 10)).willReturn(List.of(topper));

        mockMvc.perform(get("/api/results/toppers")
                        .param("departmentId", "1")
                        .param("semester", "3")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Alice Johnson"))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].grade").value("S"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  POST /api/results
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/results — returns 201 on successful result creation")
    void addResult_returns201() throws Exception {
        ResultDTO.Request request = ResultDTO.Request.builder()
                .studentId(10L).courseId(5L).semester(4)
                .marks(new BigDecimal("87.50")).grade("A")
                .examDate(LocalDate.of(2024, 5, 20))
                .build();
        ResultDTO.Response response = ResultDTO.Response.builder()
                .id(100L).studentId(10L).semester(4)
                .marks(new BigDecimal("87.50")).grade("A")
                .examDate(LocalDate.of(2024, 5, 20))
                .build();

        given(resultService.addResult(any())).willReturn(response);

        mockMvc.perform(post("/api/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.grade").value("A"));
    }

    @Test
    @DisplayName("POST /api/results — returns 409 on duplicate result")
    void addResult_returns409OnDuplicate() throws Exception {
        ResultDTO.Request request = ResultDTO.Request.builder()
                .studentId(10L).courseId(5L).semester(4)
                .marks(new BigDecimal("87.50")).grade("A")
                .examDate(LocalDate.of(2024, 5, 20))
                .build();

        given(resultService.addResult(any()))
                .willThrow(new DuplicateResourceException(
                        "Result already exists for this student, course and semester"));

        mockMvc.perform(post("/api/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/results — returns 400 when request body missing required fields")
    void addResult_returns400OnValidationFailure() throws Exception {
        // Missing studentId, courseId, examDate
        String invalidBody = """
                {
                    "semester": 4,
                    "marks": 87.50,
                    "grade": "A"
                }
                """;

        mockMvc.perform(post("/api/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }
}
