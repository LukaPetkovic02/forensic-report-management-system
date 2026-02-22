package com.example.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForensicReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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

    private String filePath;
}
