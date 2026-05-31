package com.studentvault.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BenchmarkDTO {

    private String queryDescription;
    private long withoutIndexMs;
    private long withIndexMs;
    private String improvement;
    private String queryPlan;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BenchmarkReport {
        private String generatedAt;
        private long totalStudents;
        private long totalResults;
        private List<BenchmarkDTO> benchmarks;
        private String summary;
    }
}
