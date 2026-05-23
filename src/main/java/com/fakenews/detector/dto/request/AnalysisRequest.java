package com.fakenews.detector.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalysisRequest {

    private String url;

    private String articleText;

    @NotBlank(message = "Either URL or article text is required")
    private String content;
}