package com.fakenews.detector.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse implements java.io.Serializable {
    private String claim;
    private String rating;
    private String source;
    private String explanation;
}