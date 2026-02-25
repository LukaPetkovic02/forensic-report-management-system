package com.example.backend.model;

import com.example.backend.deserializer.GeoPointDeserializer;
import com.example.backend.dto.LocationDTO;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Document(indexName = "forensic_reports")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForensicReportDocument {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "serbian_custom")
    private String organizationName;

    @Field(type = FieldType.Keyword)
    private String classification;

    @Field(type = FieldType.Keyword)
    private String hash;

    @Field(type = FieldType.Text,  analyzer = "serbian_custom")
    private String threatName;

    @Field(type = FieldType.Text, analyzer = "serbian_custom")
    private String behaviorDescription;

    @Field(type = FieldType.Text, analyzer = "serbian_custom")
    private String forensicExpert1;

    @Field(type = FieldType.Text, analyzer = "serbian_custom")
    private String forensicExpert2;

    // FULL TEXT (PDF sadržaj)
    @Field(type = FieldType.Text, analyzer = "serbian_custom")
    private String reportText;

    @GeoPointField
    private LocationDTO location;

    @Field(type = FieldType.Dense_Vector, dims = 384, similarity = "cosine")
    private float[] embedding;
}
