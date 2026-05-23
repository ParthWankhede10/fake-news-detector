package com.fakenews.detector.controller;

import com.fakenews.detector.dto.request.AnalysisRequest;
import com.fakenews.detector.dto.response.AnalysisResponse;
import com.fakenews.detector.entity.Analysis;
import com.fakenews.detector.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(
            @Valid @RequestBody AnalysisRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Set content field from url or articleText
        if (request.getContent() == null || request.getContent().isBlank()) {
            if (request.getUrl() != null && !request.getUrl().isBlank()) {
                request.setContent(request.getUrl());
            } else if (request.getArticleText() != null && !request.getArticleText().isBlank()) {
                request.setContent(request.getArticleText());
            }
        }

        AnalysisResponse response = analysisService.analyze(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<Analysis>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Analysis> history = analysisService.getUserHistory(
                userDetails.getUsername(), pageable);
        return ResponseEntity.ok(history);
    }
}