package com.example.backend.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.backend.configuration.QueryTokenizer;
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
    private final ElasticsearchClient esClient;
    private final BooleanQueryParser booleanQueryParser;

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

    public List<ForensicReportDocument> searchReports(String expert, String hash, String classification) throws IOException {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        if(expert != null && !expert.isEmpty()){
            bool.must(q -> q
                    .bool(b -> b
                            .should(s -> s.term(t -> t.field("forensicExpert1").value(expert)))
                            .should(s -> s.term(t -> t.field("forensicExpert2").value(expert)))
                    )
            );
        }

        if(hash != null && !hash.isEmpty()){
            bool.must(q -> q.term(t -> t.field("hash").value(hash)));
        }

        if(classification != null && !classification.isEmpty()){
            bool.must(q -> q.term(t -> t.field("classification").value(classification)));
        }

        SearchRequest request = new SearchRequest.Builder()
                .index("forensic_reports")
                .query(q -> q.bool(bool.build()))
                .size(100)
                .build();

        SearchResponse<ForensicReportDocument> response = esClient.search(request, ForensicReportDocument.class);

        return response.hits().hits().stream()
                .map(hit -> hit.source())
                .toList();
    }

    public List<ForensicReportDocument> search(String query) throws IOException {

        Query parsedQuery = booleanQueryParser.parse(query);

        SearchResponse<ForensicReportDocument> response =
                esClient.search(s -> s
                                .index("forensic_reports")
                                .query(parsedQuery)
                                .size(100),
                        ForensicReportDocument.class
                );

        return response.hits()
                .hits()
                .stream()
                .map(hit -> hit.source())
                .toList();
    }

    private String cleanToken(String token) {
        if (token.startsWith("\"") && token.endsWith("\"")) {
            return token.substring(1, token.length() - 1);
        }
        return token;
    }

}
