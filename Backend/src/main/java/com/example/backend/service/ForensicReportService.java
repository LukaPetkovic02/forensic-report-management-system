package com.example.backend.service;

import com.example.backend.dto.ForensicReportDTO;
import com.example.backend.model.ForensicReport;
import com.example.backend.model.ForensicReportDocument;
import com.example.backend.repository.ForensicReportElasticRepository;
import com.example.backend.repository.ForensicReportRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForensicReportService {
    private final PdfParserService pdfParserService;
    private final MinioService minioService;
    private final ForensicReportRepository forensicReportRepository;
    private final ForensicReportElasticRepository elasticRepository;

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

        ForensicReport saved = forensicReportRepository.save(entity);

        ForensicReportDocument doc = mapToDocument(saved);
        doc.setId(saved.getId().toString());

        elasticRepository.save(doc);

        return saved;
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

    private ForensicReportDocument mapToDocument(ForensicReport entity){
        ForensicReportDocument doc = new ForensicReportDocument();

        doc.setHash(entity.getHash());
        doc.setClassification(entity.getClassification());
        doc.setOrganizationName(entity.getOrganizationName());
        doc.setBehaviorDescription(entity.getBehaviorDescription());
        doc.setThreatName(entity.getThreatName());

        doc.setForensicExpert1(entity.getForensicExpert1());
        doc.setForensicExpert2(entity.getForensicExpert2());

        return doc;
    }

    public List<ForensicReportDocument> searchReports(String expert, String hash, String classification) {
        if(expert != null && !expert.isEmpty()) {
            return elasticRepository.findByForensicExpert1OrForensicExpert2(expert, expert);
        } else if(hash != null && !hash.isEmpty()) {
            return elasticRepository.findByHash(hash).map(List::of).orElse(List.of());
        } else if(classification != null && !classification.isEmpty()) {
            return elasticRepository.findByClassification(classification);
        } else {
            return List.of();
        }
    }

}
