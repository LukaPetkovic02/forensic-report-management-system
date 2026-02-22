package com.example.backend.model;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "forensic_reports")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForensicReportDocument {
    @Id
    private String id;

    private String organizationName;
    private String classification;
    private String hash;
    private String threatName;
    private String behaviorDescription;

    private String forensicExpert1;
    private String forensicExpert2;
}
