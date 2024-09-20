package com.vesanieminen.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;

@Service
@Slf4j
public class LiukuriApiClient {

    private final RestTemplate restTemplate;
    private final String apiUrl = "https://liukuri.fi/api/calculateCost";

    public LiukuriApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public CalculationResponse calculateCost() {
        try {
            // Prepare the request data
            CalculationRequest request = new CalculationRequest();
            LinkedHashMap<Long, Double> consumptionData = new LinkedHashMap<>();
            // Add 1-hour interval data
            consumptionData.put(1696953600000L, 5.0); // 2023-10-10T16:00:00Z
            consumptionData.put(1696957200000L, 6.0); // 2023-10-10T17:00:00Z
            consumptionData.put(1696960800000L, 4.5); // 2023-10-10T18:00:00Z
            consumptionData.put(1696964400000L, 5.5); // 2023-10-10T19:00:00Z
            consumptionData.put(1696968000000L, 6.2); // 2023-10-10T20:00:00Z
            request.setConsumptionData(consumptionData);
            request.setMargin(0.1);
            request.setVat(true);

            // Set the headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create the request entity
            HttpEntity<CalculationRequest> entity = new HttpEntity<>(request, headers);

            // Make the POST request
            ResponseEntity<CalculationResponse> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    CalculationResponse.class
            );

            // Return the response body
            return response.getBody();

        } catch (Exception e) {
            log.info("Error when calculating costs: %s".formatted(e.toString()));
        }

        return null;
    }

    public CalculationResponse calculateCost(LinkedHashMap<Long, Double> consumptionData, double margin, boolean vat) {
        try {
            // Prepare the request data
            CalculationRequest request = new CalculationRequest(consumptionData, margin, vat);

            // Set the headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create the request entity
            HttpEntity<CalculationRequest> entity = new HttpEntity<>(request, headers);

            // Make the POST request
            ResponseEntity<CalculationResponse> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    CalculationResponse.class
            );

            // Return the response body
            return response.getBody();

        } catch (Exception e) {
            log.info("Error when calculating costs: %s".formatted(e.toString()));
        }

        return null;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CalculationRequest {
        private LinkedHashMap<Long, Double> consumptionData;
        private double margin;
        private boolean vat;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class CalculationResponse {
        private double totalCost;
        private double averagePrice;
    }

}
