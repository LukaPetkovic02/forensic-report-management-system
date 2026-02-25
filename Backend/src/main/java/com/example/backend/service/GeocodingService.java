package com.example.backend.service;

import com.example.backend.dto.NominatimResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final WebClient webClient;

    public GeoPoint getCoordinates(String address){
        List<NominatimResponse> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("nominatim.openstreetmap.org")
                        .path("/search")
                        .queryParam("q",address)
                        .queryParam("format","json")
                        .queryParam("limit",1)
                        .build())
                .header("User-Agent", "forensic-report-system")
                .retrieve()
                .bodyToFlux(NominatimResponse.class)
                .collectList()
                .block();

        if(response == null || response.isEmpty()){
            throw new RuntimeException("Address not found: "+ address);
        }

        double lat = Double.parseDouble(response.get(0).getLat());
        double lon = Double.parseDouble(response.get(0).getLon());

        return new GeoPoint(lat, lon);
    }
}
