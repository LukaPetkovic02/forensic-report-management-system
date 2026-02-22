package com.example.backend.controller;

import com.example.backend.dto.ForensicReportDTO;
import com.example.backend.model.ForensicReport;
import com.example.backend.model.ForensicReportDocument;
import com.example.backend.service.ForensicReportService;
import com.example.backend.service.PdfParserService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    @Autowired
    private PdfParserService pdfParserService;

    @Autowired
    private ForensicReportService forensicReportService;

    @PostMapping("/parse")
    public ResponseEntity<ForensicReportDTO> parseReport(@RequestParam("file") MultipartFile file){
        try (PDDocument document = PDDocument.load(file.getInputStream())){
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            ForensicReportDTO dto = pdfParserService.parse(text);

            return ResponseEntity.ok(dto);
        } catch(IOException e){
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<ForensicReport> uploadReport(
            @RequestParam("file") MultipartFile file, @RequestPart("dto") ForensicReportDTO dto) {

        ForensicReport saved = forensicReportService.saveFromPdf(file, dto);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ForensicReportDocument>> searchReports(
            @RequestParam(required = false) String expert,
            @RequestParam(required = false) String hash,
            @RequestParam(required = false) String classification
    ){
        List<ForensicReportDocument> results = forensicReportService.searchReports(expert, hash, classification);
        return ResponseEntity.ok(results);
    }
}
