package com.fakenews.detector.service;

import com.fakenews.detector.dto.request.AnalysisRequest;
import com.fakenews.detector.dto.response.AnalysisResponse;
import com.fakenews.detector.dto.response.ClaimResponse;
import com.fakenews.detector.entity.Analysis;
import com.fakenews.detector.entity.User;
import com.fakenews.detector.repository.AnalysisRepository;
import com.fakenews.detector.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final OpenAiAnalysisService openAiAnalysisService;
    private final GoogleFactCheckService googleFactCheckService;
    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Cacheable(value = "analyses", key = "#request.content.hashCode()")
    public AnalysisResponse analyze(AnalysisRequest request, String userEmail) {
        log.info("Running fresh analysis for content hash: {}", request.getContent().hashCode());

        // Step 1 - Get AI analysis
        AnalysisResponse aiResponse = openAiAnalysisService.analyzeArticle(request.getContent());

        // Step 2 - Get Google Fact Check results
        List<ClaimResponse> factCheckClaims = googleFactCheckService.checkClaims(request.getContent());

        // Step 3 - Merge results
        List<ClaimResponse> mergedClaims = mergeClaims(aiResponse.getClaims(), factCheckClaims);

        // Step 4 - Build final response
        AnalysisResponse finalResponse = AnalysisResponse.builder()
                .verdict(aiResponse.getVerdict())
                .confidenceScore(aiResponse.getConfidenceScore())
                .summary(aiResponse.getSummary())
                .claims(mergedClaims)
                .build();

        // Step 5 - Save to database
        saveAnalysis(request, finalResponse, userEmail);

        return finalResponse;
    }

    private List<ClaimResponse> mergeClaims(List<ClaimResponse> aiClaims,
                                            List<ClaimResponse> factCheckClaims) {
        // Add all AI claims first
        java.util.ArrayList<ClaimResponse> merged = new java.util.ArrayList<>(aiClaims);
        // Add fact check claims that aren't duplicates
        for (ClaimResponse factClaim : factCheckClaims) {
            boolean isDuplicate = aiClaims.stream()
                    .anyMatch(ai -> ai.getClaim().toLowerCase()
                            .contains(factClaim.getClaim().toLowerCase().substring(0,
                                    Math.min(20, factClaim.getClaim().length()))));
            if (!isDuplicate) {
                merged.add(factClaim);
            }
        }
        return merged;
    }

    private void saveAnalysis(AnalysisRequest request,
                              AnalysisResponse response,
                              String userEmail) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            String claimsJson = objectMapper.writeValueAsString(response.getClaims());

            Analysis analysis = Analysis.builder()
                    .articleUrl(request.getUrl())
                    .articleText(request.getArticleText())
                    .verdict(response.getVerdict())
                    .confidenceScore(response.getConfidenceScore())
                    .claimsBreakdown(claimsJson)
                    .user(user)
                    .build();

            analysisRepository.save(analysis);
        } catch (Exception e) {
            log.error("Failed to save analysis: {}", e.getMessage());
        }
    }

    public Page<Analysis> getUserHistory(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return analysisRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
}