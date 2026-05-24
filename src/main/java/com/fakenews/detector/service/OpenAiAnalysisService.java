package com.fakenews.detector.service;

import com.fakenews.detector.dto.response.AnalysisResponse;
import com.fakenews.detector.dto.response.ClaimResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiAnalysisService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper;

    public AnalysisResponse analyzeArticle(String content) {
        String prompt = """
                You are a professional fact-checker. Analyze this news for credibility.
                Return ONLY this JSON, no extra text:
                {
                  "verdict": "REAL or FAKE or UNCERTAIN",
                  "confidenceScore": 75,
                  "summary": "2-3 sentence assessment",
                  "claims": [
                    {
                      "claim": "specific claim",
                      "rating": "TRUE or FALSE or UNVERIFIED or MISLEADING",
                      "source": "AI Analysis",
                      "explanation": "reason"
                    }
                  ]
                }
                Article: %s
                """.formatted(content);

        try {
            RestClient client = RestClient.create();

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

            Map response = client.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            String text = extractText(response);
            log.info("Gemini response: {}", text);
            return parseResponse(text);

        } catch (Exception e) {
            log.error("Gemini API error: {}", e.getMessage(), e);
            throw new RuntimeException("AI analysis failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map response) {
        List<Map> candidates = (List<Map>) response.get("candidates");
        Map content = (Map) candidates.get(0).get("content");
        List<Map> parts = (List<Map>) content.get("parts");
        return (String) parts.get(0).get("text");
    }

    @SuppressWarnings("unchecked")
    private AnalysisResponse parseResponse(String response) {
        try {
            String cleaned = response.trim()
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            Map<String, Object> map = objectMapper.readValue(cleaned, Map.class);

            String verdict = (String) map.get("verdict");
            Double confidenceScore = ((Number) map.get("confidenceScore")).doubleValue();
            String summary = (String) map.get("summary");

            List<Map<String, String>> claimsRaw = (List<Map<String, String>>) map.get("claims");
            List<ClaimResponse> claims = claimsRaw.stream()
                    .map(c -> ClaimResponse.builder()
                            .claim(c.get("claim"))
                            .rating(c.get("rating"))
                            .source(c.get("source"))
                            .explanation(c.get("explanation"))
                            .build())
                    .toList();

            return AnalysisResponse.builder()
                    .verdict(verdict)
                    .confidenceScore(confidenceScore)
                    .summary(summary)
                    .claims(claims)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse response: {}", e.getMessage());
            return AnalysisResponse.builder()
                    .verdict("UNCERTAIN")
                    .confidenceScore(0.0)
                    .summary("Analysis failed due to parsing error.")
                    .claims(List.of())
                    .build();
        }
    }
}