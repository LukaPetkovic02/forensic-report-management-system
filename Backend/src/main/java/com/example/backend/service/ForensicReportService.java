package com.example.backend.service;

import ai.djl.translate.TranslateException;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.util.NamedValue;
import com.example.backend.dto.ForensicReportDTO;
import com.example.backend.dto.LocationDTO;
import com.example.backend.dto.PageResponse;
import com.example.backend.dto.SearchResultDTO;
import com.example.backend.model.ForensicReport;
import com.example.backend.model.ForensicReportDocument;
import com.example.backend.repository.ForensicReportElasticRepository;
import com.example.backend.repository.ForensicReportRepository;
import com.example.backend.util.VectorizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private final GeocodingService geocodingService;

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

        GeoPoint geoPoint = geocodingService.getCoordinates(dto.getAddress());
        doc.setLocation(LocationDTO.builder()
                .lat(geoPoint.getLat())
                .lon(geoPoint.getLon())
                .build());

        elasticRepository.save(doc);

        log.info("REPORT_UPLOADED | city={} | threat={} | expert1={} | expert2={}",
                dto.getAddress().split(",")[2].trim(),
                dto.getThreatName(),
                dto.getForensicExpert1(),
                dto.getForensicExpert2());

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

    public PageResponse<ForensicReportDocument> searchBasic(String input, int page, int size) throws IOException { // by forensics, classification, hash

        int from = page * size;

        SearchResponse<ForensicReportDocument> response =
                esClient.search(s -> s
                                .index("forensic_reports")
                                .from(from)
                                .size(size)
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
                                )),
                        ForensicReportDocument.class
                );

        return getForensicReportDocumentPageResponse(page, size, response);
    }

    private PageResponse<ForensicReportDocument> getForensicReportDocumentPageResponse(int page, int size, SearchResponse<ForensicReportDocument> response) {
        List<ForensicReportDocument> content =
                response.hits()
                        .hits()
                        .stream()
                        .map(hit -> hit.source())
                        .toList();

        long totalElements = response.hits().total() != null
                ? response.hits().total().value()
                : content.size();

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<ForensicReportDocument>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .page(page)
                .size(size)
                .build();
    }

    public PageResponse<SearchResultDTO> searchOrganizationThreatWithHighlight(String input, int page, int size) throws IOException {

        int from = page * size;
        SearchResponse<ForensicReportDocument> response =
                esClient.search(s -> s
                                .index("forensic_reports")
                                .from(from)
                                .size(size)
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
                                .highlight(h -> h
                                        .preTags("<mark>")
                                        .postTags("</mark>")
                                        .fields(NamedValue.of("organizationName", HighlightField.of(f -> f)),
                                                NamedValue.of("threatName", HighlightField.of(f -> f)))
                                ),
                        ForensicReportDocument.class
                );

        return getSearchResultDTOPageResponse(page, size, response);
    }

    private PageResponse<SearchResultDTO> getSearchResultDTOPageResponse(int page, int size, SearchResponse<ForensicReportDocument> response) {
        List<SearchResultDTO> content = buildDTOResponse(response);

        long totalElements = response.hits().total() != null
                ? response.hits().total().value()
                : content.size();

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<SearchResultDTO>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .page(page)
                .size(size)
                .build();
    }

    public PageResponse<SearchResultDTO> searchBehaviorDescription(String input, int page, int size) throws IOException {
        int from = page * size;

        SearchResponse<ForensicReportDocument> response =
                esClient.search(s -> s
                                .index("forensic_reports")
                                .from(from)
                                .size(size)
                                .query(q -> q.multiMatch(mm -> mm
                                        .query(input)
                                        .fields(
                                                "behaviorDescription"
                                                //"threatName",
                                                //"organizationName",
                                                //"forensicExpert1",
                                                //"forensicExpert2"
                                        )
                                ))
                                .highlight(h -> h
                                        .preTags("<mark>")
                                        .postTags("</mark>")
                                        .fields(
                                                NamedValue.of("behaviorDescription", HighlightField.of(f -> f))
                                        )
                                ),
                        ForensicReportDocument.class
                );

        return getSearchResultDTOPageResponse(page, size, response);
    }

    public PageResponse<ForensicReportDocument> knnSearch(String input, int page, int size)
            throws IOException, TranslateException {

        float[] embedding = VectorizationUtil.getEmbedding(input);

        List<Float> floatList = new ArrayList<>();
        for (float v : embedding) {
            floatList.add(v);
        }

        int from = page * size;

        SearchResponse<ForensicReportDocument> response =
                esClient.search(s -> s
                                .index("forensic_reports")
                                .from(from)
                                .size(size)
                                .knn(k -> k
                                        .field("embedding")
                                        .queryVector(floatList)
                                        .k(100)
                                        .numCandidates(200)
                                ),
                        ForensicReportDocument.class
                );

        return getForensicReportDocumentPageResponse(page, size, response);
    }



    public PageResponse<SearchResultDTO> searchWithHighlight(String query, int page, int size) throws IOException {

        Query parsedQuery = booleanQueryParser.parse(query);

        int from = page * size;

        SearchResponse<ForensicReportDocument> response =
                esClient.search(s -> s
                                .index("forensic_reports")
                                .from(from)
                                .size(size)
                                .query(parsedQuery)
                                .highlight(h -> h
                                        .preTags("<mark>")
                                        .postTags("</mark>")
                                        .fields(
                                                NamedValue.of("behaviorDescription", HighlightField.of(f -> f)),
                                                NamedValue.of("threatName", HighlightField.of(f -> f)),
                                                NamedValue.of("organizationName",HighlightField.of(f -> f))
                                        )
                                ),
                        ForensicReportDocument.class
                );

        return getSearchResultDTOPageResponse(page, size, response);
    }

    private String shorten(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private List<SearchResultDTO> buildDTOResponse(
            SearchResponse<ForensicReportDocument> response) {

        List<SearchResultDTO> results = new ArrayList<>();

        response.hits().hits().forEach(hit -> {

            ForensicReportDocument source = hit.source();
            String snippet = null;

            if (hit.highlight() != null) {

                if (hit.highlight().containsKey("behaviorDescription")) {
                    snippet = String.join(" ... ",
                            hit.highlight().get("behaviorDescription"));

                } else if (hit.highlight().containsKey("threatName")) {
                    snippet = String.join(" ... ",
                            hit.highlight().get("threatName"));

                } else if (hit.highlight().containsKey("organizationName")) {
                    snippet = String.join(" ... ",
                            hit.highlight().get("organizationName"));
                }
            }

            if (snippet == null && source.getBehaviorDescription() != null) {
                snippet = shorten(source.getBehaviorDescription(), 200);
            }

            results.add(
                    SearchResultDTO.builder()
                            .id(source.getId())
                            .organizationName(source.getOrganizationName())
                            .threatName(source.getThreatName())
                            .classification(source.getClassification())
                            .hash(source.getHash())
                            .snippet(snippet)
                            .score(hit.score() != null ? hit.score().floatValue() : 0f)
                            .build()
            );
        });

        return results;
    }

}
