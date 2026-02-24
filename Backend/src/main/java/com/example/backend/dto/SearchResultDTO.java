package com.example.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResultDTO {

    private String id;
    private String organizationName;
    private String threatName;
    private String classification;
    private String hash;

    private String snippet; // highlight ili skraceni text
    private float score;
}
