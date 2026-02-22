package com.example.backend.service;

import com.example.backend.dto.ForensicReportDTO;
import com.example.backend.model.ForensicReport;
import com.example.backend.repository.ForensicReportRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ForensicReportService {
    private final PdfParserService pdfParserService;
    private final MinioService minioService;
    private final ForensicReportRepository forensicReportRepository;

    public ForensicReport saveFromPdf(MultipartFile file, ForensicReportDTO dto){
        if(file == null || file.isEmpty()){
            throw new IllegalArgumentException("File is empty");
        }

        if(!file.getContentType().equalsIgnoreCase("application/pdf")){
            throw new IllegalArgumentException("File is not a PDF");
        }

        String objectName = minioService.uploadFile(file);

        ForensicReport entity = mapToEntity(dto);
        entity.setFilePath(objectName);

        return forensicReportRepository.save(entity);
    }

    private ForensicReport mapToEntity(ForensicReportDTO dto) {
        ForensicReport report = new ForensicReport();

        report.setOrganizationName(dto.getOrganizationName());
        report.setAddress(dto.getAddress());
        report.setEmail(dto.getEmail());
        report.setPhone(dto.getPhone());

        report.setFileName(dto.getFileName());
        report.setClassification(dto.getClassification());
        report.setHash(dto.getHash());

        report.setThreatName(dto.getThreatName());
        report.setBehaviorDescription(dto.getBehaviorDescription());

        report.setForensicExpert1(dto.getForensicExpert1());
        report.setForensicExpert2(dto.getForensicExpert2());

        return report;
    }

//    private void validateParsedReport(ForensicReportDTO dto) {
//
//        if (dto.getOrganizationName() == null)
//            throw new IllegalArgumentException("Organizacija nije parsirana.");
//
//        if (dto.getHash() == null)
//            throw new IllegalArgumentException("Hash nije parsiran.");
//
//        if (dto.getClassification() == null)
//            throw new IllegalArgumentException("Klasifikacija nije parsirana.");
//
//        if (dto.getForensicExpert1() == null)
//            throw new IllegalArgumentException("Forenzičar nije parsiran.");
//    }
}
