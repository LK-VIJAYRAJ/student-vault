package com.studentvault.controller;

import com.studentvault.dto.BenchmarkDTO;
import com.studentvault.service.BenchmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    /**
     * The standout endpoint.
     * Hit GET /api/benchmark/run during your demo or interview.
     * It returns real before/after timings showing how indexes
     * improved query performance on 50,000 student records.
     */
    @GetMapping("/run")
    public ResponseEntity<BenchmarkDTO.BenchmarkReport> runBenchmark() {
        return ResponseEntity.ok(benchmarkService.runBenchmark());
    }
}
