package com.example.backend.controller;

import ai.djl.translate.TranslateException;
import com.example.backend.dto.ForensicReportDTO;
import com.example.backend.dto.PageResponse;
import com.example.backend.dto.SearchResultDTO;
import com.example.backend.model.ForensicReport;
import com.example.backend.model.ForensicReportDocument;
import com.example.backend.service.ForensicReportService;
import com.example.backend.service.GeoSearchService;
import com.example.backend.service.GeocodingService;
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

    @Autowired
    private GeoSearchService geoSearchService;

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

    @GetMapping("/search-basic") // forenzicar/hash/klasifikacija
    public PageResponse<ForensicReportDocument> searchBasic(@RequestParam String input,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) throws IOException {
        return forensicReportService.searchBasic(input, page, size);
    }

    @GetMapping("/search-org-threat")
    public PageResponse<SearchResultDTO> searchOrganizationThreat(@RequestParam String input,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) throws IOException {
        return forensicReportService.searchOrganizationThreatWithHighlight(input, page, size);
    }

    @GetMapping("/search/behavior")
    public PageResponse<SearchResultDTO> searchBehavior(@RequestParam String input,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) throws IOException {
        return forensicReportService.searchBehaviorDescription(input, page, size);
    }

    @GetMapping("/search")
    public PageResponse<SearchResultDTO> search(@RequestParam String query,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) throws IOException {
        return forensicReportService.searchWithHighlight(query, page, size);
    }

    @GetMapping("/search/knn")
    public PageResponse<ForensicReportDocument> searchKnn(@RequestParam String input,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) throws IOException, TranslateException {
        return forensicReportService.knnSearch(input, page, size);
    }

    @GetMapping("/search/geo/location")
    public PageResponse<ForensicReportDocument> searchByLocation(
            @RequestParam String location,
            @RequestParam double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return geoSearchService.searchWithinRadius(location, radiusKm, page, size);
    }
}
