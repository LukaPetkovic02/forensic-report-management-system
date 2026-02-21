package com.example.backend.controller;

import com.example.backend.dto.ForensicReportDTO;
import com.example.backend.service.PdfParserService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class ReportController {
    @Autowired
    private PdfParserService pdfParserService;

    @PostMapping("/api/reports/parse")
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
}
