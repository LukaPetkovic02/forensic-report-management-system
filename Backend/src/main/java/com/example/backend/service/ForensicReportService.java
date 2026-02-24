package com.example.backend.service;

import ai.djl.translate.TranslateException;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnQuery;
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
import com.example.backend.util.VectorizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForensicReportService {
    private final PdfParserService pdfParserService;
    private final MinioService minioService;
    private final ForensicReportRepository forensicReportRepository;
    private final ForensicReportElasticRepository elasticRepository;
    private final ElasticsearchClient esClient;
    private final BooleanQueryParser booleanQueryParser;
    private final VectorizationUtil vectorizationUtil;

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

        String textForEmbedding =
                entity.getOrganizationName() + " " +
                        entity.getThreatName() + " " +
                        entity.getBehaviorDescription() + " " +
                        entity.getForensicExpert1() + " " +
                        entity.getForensicExpert2();

        try {
            float[] embeddingArray = vectorizationUtil.getEmbedding(textForEmbedding);
            // log.info("Embedding length: {}", embeddingArray.length);
            doc.setEmbedding(embeddingArray);

        } catch (Exception e) {
            log.error("Embedding generation failed", e);
        }

        return doc;
    }

    public List<ForensicReportDocument> searchBasic(String input) throws IOException {

        SearchResponse<ForensicReportDocument> response =
                esClient.search(s -> s
                                .index("forensic_reports")
                                .query(q -> q.bool(b -> b
                                        .should(sh -> sh.multiMatch(mm -> mm
                                                .query(input)
                                                .fields(
                                                        "forensicExpert1",
                                                        "forensicExpert2"
                                                )
                                        ))
                                        .should(sh -> sh.term(t -> t
                                                .field("hash")
                                                .value(input)
                                        ))
                                        .should(sh -> sh.term(t -> t
                                                .field("classification")
                                                .value(input)
                                        ))
                                        .minimumShouldMatch("1")
                                ))
                                .size(100),
                        ForensicReportDocument.class
                );

        return response.hits()
                .hits()
                .stream()
                .map(hit -> hit.source())
                .toList();
    }

    public List<ForensicReportDocument> searchOrganizationThreat(String input) throws IOException {

        SearchResponse<ForensicReportDocument> response =
                esClient.search(s -> s
                                .index("forensic_reports")
                                .query(q -> q.bool(b -> b
                                        .should(sh -> sh.match(m -> m
                                                .field("organizationName")
                                                .query(input)
                                        ))
                                        .should(sh -> sh.match(m -> m
                                                .field("threatName")
                                                .query(input)
                                        ))
                                        .minimumShouldMatch("1")
                                ))
                                .size(100),
                        ForensicReportDocument.class
                );

        return response.hits()
                .hits()
                .stream()
                .map(hit -> hit.source())
                .toList();
    }

    public List<ForensicReportDocument> searchBehaviorDescription(String input) throws IOException {
        SearchResponse<ForensicReportDocument> response =
                esClient.search(s -> s
                                .index("forensic_reports")
                                .query(q -> q.match(m -> m
                                        .field("behaviorDescription")
                                        .query(input)
                                ))
                                .size(100),
                        ForensicReportDocument.class
                );

        return response.hits()
                .hits()
                .stream()
                .map(hit -> hit.source())
                .toList();
    }

    public List<ForensicReportDocument> knnSearch(String input)
            throws IOException, TranslateException {

        float[] embedding = VectorizationUtil.getEmbedding(input);

        List<Float> floatList = new ArrayList<>();
        for (float v : embedding) {
            floatList.add(v);
        }

        SearchResponse<ForensicReportDocument> response =
                esClient.search(s -> s
                                .index("forensic_reports")
                                .knn(k -> k
                                        .field("embedding")
                                        .queryVector(floatList)
                                        .k(10)
                                        .numCandidates(100)
                                )
                                .size(5),
                        ForensicReportDocument.class
                );

        return response.hits()
                .hits()
                .stream()
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

}
