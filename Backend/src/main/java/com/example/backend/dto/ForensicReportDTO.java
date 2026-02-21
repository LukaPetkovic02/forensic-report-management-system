package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForensicReportDTO {
    private String organizationName;
    private String address;
    private String email;
    private String phone;

    private String fileName;
    private String classification;
    private String hash;

    private String threatName;
    private String behaviorDescription;

    private String forensicExpert1;
    private String forensicExpert2;
}
