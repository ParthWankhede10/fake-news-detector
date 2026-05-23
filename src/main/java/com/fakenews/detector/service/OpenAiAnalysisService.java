package com.fakenews.detector.service;

import com.fakenews.detector.dto.response.AnalysisResponse;
import com.fakenews.detector.dto.response.ClaimResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiAnalysisService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public AnalysisResponse analyzeArticle(String content) {
        ChatClient chatClient = chatClientBuilder.build();

        String prompt = """
            You are a professional fact-checker and misinformation analyst.
            
            Analyze the following news article or content for credibility and misinformation.
            
            Return your response in this EXACT JSON format with no extra text:
            {
              "verdict": "REAL or FAKE or UNCERTAIN",
              "confidenceScore": 75,
              "summary": "2-3 sentence overall assessment",
              "claims": [
                {
                  "claim": "specific claim from the article",
                  "rating": "TRUE or FALSE or UNVERIFIED or MISLEADING",
                  "source": "AI Analysis",
                  "explanation": "why this claim is rated this way"
                }
              ]
            }
            
            Article to analyze:
            %s
            """.formatted(content);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.info("Gemini response: {}", response);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("Gemini API error: {}", e.getMessage(), e);
            throw e;
        }
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
            log.error("Failed to parse OpenAI response: {}", e.getMessage());
            return AnalysisResponse.builder()
                    .verdict("UNCERTAIN")
                    .confidenceScore(0.0)
                    .summary("Analysis failed due to parsing error.")
                    .claims(List.of())
                    .build();
        }
    }
}