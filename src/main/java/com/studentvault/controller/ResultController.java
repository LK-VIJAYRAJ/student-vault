package com.studentvault.controller;

import com.studentvault.dto.ResultDTO;
import com.studentvault.service.ResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ResultDTO.Response>> getResultsByStudent(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(resultService.getResultsByStudent(studentId));
    }

    @GetMapping("/student/{studentId}/semester/{semester}")
    public ResponseEntity<List<ResultDTO.Response>> getResultsByStudentAndSemester(
            @PathVariable Long studentId,
            @PathVariable Integer semester) {
        return ResponseEntity.ok(
                resultService.getResultsByStudentAndSemester(studentId, semester));
    }

    @GetMapping("/toppers")
    public ResponseEntity<List<ResultDTO.TopperResponse>> getToppers(
            @RequestParam Long departmentId,
            @RequestParam Integer semester,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(resultService.getToppers(departmentId, semester, limit));
    }

    @GetMapping("/averages")
    public ResponseEntity<List<ResultDTO.CourseAverage>> getCourseAverages(
            @RequestParam Long departmentId) {
        return ResponseEntity.ok(resultService.getCourseAverages(departmentId));
    }

    @GetMapping("/grade-distribution")
    public ResponseEntity<List<ResultDTO.GradeDistribution>> getGradeDistribution(
            @RequestParam Integer semester) {
        return ResponseEntity.ok(resultService.getGradeDistribution(semester));
    }

    @PostMapping
    public ResponseEntity<ResultDTO.Response> addResult(
            @Valid @RequestBody ResultDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resultService.addResult(request));
    }
}
