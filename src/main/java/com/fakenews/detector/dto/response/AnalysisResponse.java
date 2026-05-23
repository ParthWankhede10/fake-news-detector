package com.fakenews.detector.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse implements java.io.Serializable {
    private Long id;
    private String verdict;
    private Double confidenceScore;
    private List<ClaimResponse> claims;
    private String summary;
    private LocalDateTime analyzedAt;
}