package com.example.backend.service;

import co.elastic.clients.elasticsearch._types.LatLonGeoLocation;
import com.example.backend.dto.PageResponse;
import com.example.backend.model.ForensicReportDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeoSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final GeocodingService geocodingService;

    public PageResponse<ForensicReportDocument> searchWithinRadius(String location, double radius, int page, int size){
        GeoPoint center = geocodingService.getCoordinates(location);

        Pageable pageable = Pageable.ofSize(size).withPage(page);

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .geoDistance(g -> g
                                .field("location")
                                .distance(radius + "km")
                                .location(l -> l
                                        .latlon(new LatLonGeoLocation.Builder()
                                                .lat(center.getLat())
                                                .lon(center.getLon())
                                                .build()
                                        )
                                )
                        )
                )
                .withPageable(pageable)
                .build();

        SearchHits<ForensicReportDocument> hits =
                elasticsearchOperations.search(query, ForensicReportDocument.class);

        long totalHits = hits.getTotalHits(); // ukupno rezultata
        List<ForensicReportDocument> content = hits.stream()
                .map(SearchHit::getContent)
                .toList();

        int totalPages = (int) Math.ceil((double) totalHits / size);

        return PageResponse.<ForensicReportDocument>builder()
                .content(content)
                .totalElements(totalHits)
                .totalPages(totalPages)
                .page(page)
                .size(size)
                .build();
    }
}
