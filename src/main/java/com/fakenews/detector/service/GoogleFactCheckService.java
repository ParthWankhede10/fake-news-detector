package com.fakenews.detector.service;

import com.fakenews.detector.dto.response.ClaimResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleFactCheckService {

    @Value("${google.factcheck.api.key}")
    private String apiKey;

    @Value("${google.factcheck.api.url}")
    private String apiUrl;

    private final RestClient restClient = RestClient.create();

    public List<ClaimResponse> checkClaims(String query) {
        try {
            Map response = restClient.get()
                    .uri(apiUrl + "?query={query}&key={key}&languageCode=en",
                            query, apiKey)
                    .retrieve()
                    .body(Map.class);

            return parseClaims(response);
        } catch (Exception e) {
            log.warn("Google Fact Check API failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ClaimResponse> parseClaims(Map response) {
        List<ClaimResponse> results = new ArrayList<>();

        if (response == null || !response.containsKey("claims")) {
            return results;
        }

        List<Map> claims = (List<Map>) response.get("claims");

        for (Map claim : claims) {
            String claimText = (String) claim.get("text");
            List<Map> reviews = (List<Map>) claim.get("claimReview");

            if (reviews != null && !reviews.isEmpty()) {
                Map review = reviews.get(0);
                String rating = (String) review.get("textualRating");
                Map publisher = (Map) review.get("publisher");
                String source = publisher != null ? (String) publisher.get("name") : "Unknown";
                String url = (String) review.get("url");

                results.add(ClaimResponse.builder()
                        .claim(claimText)
                        .rating(rating)
                        .source(source)
                        .explanation(url)
                        .build());
            }
        }
        return results;
    }
}