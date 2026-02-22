package com.example.backend.repository;

import com.example.backend.model.ForensicReportDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForensicReportElasticRepository extends ElasticsearchRepository<ForensicReportDocument, String> {
    Optional<ForensicReportDocument> findByHash(String hash);
    List<ForensicReportDocument> findByClassification(String classification);
    List<ForensicReportDocument> findByForensicExpert1OrForensicExpert2(String forensicExpert1, String forensicExpert2);
}
