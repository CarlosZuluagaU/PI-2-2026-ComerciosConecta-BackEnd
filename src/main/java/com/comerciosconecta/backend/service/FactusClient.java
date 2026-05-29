package com.comerciosconecta.backend.service;

import com.comerciosconecta.backend.dto.FactusBillRequest;
import com.comerciosconecta.backend.dto.FactusValidationResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class FactusClient {

    private static final Logger log = LoggerFactory.getLogger(FactusClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private final String username;
    private final String password;
    private final boolean mockMode;

    private final AtomicReference<String> cachedToken = new AtomicReference<>(null);
    private long tokenExpiryMillis = 0;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FactusClient(RestTemplate restTemplate,
                        @Value("${factus.base-url}") String baseUrl,
                        @Value("${factus.client-id}") String clientId,
                        @Value("${factus.client-secret}") String clientSecret,
                        @Value("${factus.username}") String username,
                        @Value("${factus.password}") String password,
                        @Value("${factus.mock-mode:false}") boolean mockMode) {

        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.password = password;
        this.mockMode = mockMode;
        if (mockMode) log.warn("⚠ FACTUS EN MODO SIMULADO — las facturas NO se envían a la DIAN");
    }

    // ============================================================================================
    //                                   TOKEN
    // ============================================================================================
    public synchronized String getAccessToken() {
        long now = System.currentTimeMillis();

        if (cachedToken.get() != null && now < tokenExpiryMillis) {
            return cachedToken.get();
        }

        log.debug("Solicitando nuevo token a Factus");
        String url = baseUrl + "/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", username);
        form.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<JsonNode> resp = restTemplate.postForEntity(url, request, JsonNode.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new RuntimeException("Error obteniendo OAuth2 token de Factus: " + resp.getStatusCode());
            }

            JsonNode body = resp.getBody();
            String token = body.path("access_token").asText(null);
            if (token == null) {
                throw new RuntimeException("No se recibió access_token de Factus");
            }

            int expires = body.path("expires_in").asInt(3600);
            cachedToken.set(token);
            tokenExpiryMillis = now + (expires - 30) * 1000L;
            log.info("Token de Factus obtenido, expira en {}s", expires);
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener token de Factus: " + e.getMessage(), e);
        }
    }

    // ============================================================================================
    //                            OBTENER RANGOS DE NUMERACIÓN
    // ============================================================================================
    public String getFirstNumberingRangeId() {
        if (mockMode) {
            log.debug("MockMode: devolviendo numbering_range_id=1");
            return "1";
        }
        String token = getAccessToken();

        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/v1/numbering-ranges")
                .queryParam("filter[is_active]", "1")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> resp = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode body = resp.getBody();

            if (body == null) {
                log.warn("Factus devolvió body null para numbering-ranges");
                return null;
            }

            JsonNode dataArray = body.path("data").path("data");

            if (!dataArray.isArray() || dataArray.isEmpty()) {
                log.warn("Factus no devolvió rangos de numeración activos");
                return null;
            }

            for (JsonNode node : dataArray) {
                String document = node.path("document").asText("");
                String id = node.path("id").asText();
                int isActive = node.path("is_active").asInt();
                if ("Factura de Venta".equals(document.trim()) && isActive == 1) {
                    log.debug("Rango de numeración encontrado: id={}", id);
                    return id;
                }
            }

            log.warn("No se encontró rango 'Factura de Venta' activo, usando fallback id=8");
            return "8";

        } catch (Exception e) {
            log.error("Error al obtener numbering ranges de Factus: {}", e.getMessage(), e);
            return "8";
        }
    }

    // ============================================================================================
    //                                      CREAR FACTURA
    // ============================================================================================
    public FactusValidationResponse crearFactura(FactusBillRequest requestDto) {
        if (mockMode) {
            log.info("MockMode: simulando factura aprobada");
            String mockId = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            Map<String, Object> bill = new HashMap<>();
            bill.put("id", mockId);
            bill.put("number", "SETP990" + System.currentTimeMillis() % 100000);
            Map<String, Object> data = new HashMap<>();
            data.put("bill", bill);
            FactusValidationResponse mock = new FactusValidationResponse();
            mock.setData(data);
            mock.setStatus("201");
            mock.setMessage("Factura simulada (mock mode)");
            return mock;
        }
        String token = getAccessToken();

        String url = baseUrl + "/v1/bills/validate";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<FactusBillRequest> entity = new HttpEntity<>(requestDto, headers);

        try {
            ResponseEntity<FactusValidationResponse> resp =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            FactusValidationResponse.class
                    );

            return resp.getBody();

        } catch (HttpStatusCodeException ex) {
            log.error("Error de Factus al crear factura - Status: {} Body: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());

            FactusValidationResponse errorResponse = new FactusValidationResponse();
            errorResponse.setStatus("ERROR");
            errorResponse.setMessage("Error de validación en Factus: " + ex.getStatusCode());

            try {
                JsonNode errorJson = objectMapper.readTree(ex.getResponseBodyAsString());
                if (errorJson.has("errors")) {
                    Map<String, List<String>> errorsMap = objectMapper.convertValue(
                            errorJson.path("errors"),
                            new TypeReference<Map<String, List<String>>>() {}
                    );
                    errorResponse.setErrors(errorsMap);
                } else if (errorJson.has("data") && errorJson.path("data").has("errors")) {
                    Map<String, List<String>> errorsMap = objectMapper.convertValue(
                            errorJson.path("data").path("errors"),
                            new TypeReference<Map<String, List<String>>>() {}
                    );
                    errorResponse.setErrors(errorsMap);
                } else if (errorJson.has("message")) {
                    errorResponse.setMessage(errorJson.path("message").asText());
                }
            } catch (Exception e) {
                errorResponse.setMessage("Error no JSON de Factus: " + ex.getResponseBodyAsString());
            }

            return errorResponse;
        } catch (Exception e) {
            log.error("Error inesperado al crear factura en Factus: {}", e.getMessage(), e);
            FactusValidationResponse errorResponse = new FactusValidationResponse();
            errorResponse.setStatus("ERROR");
            errorResponse.setMessage("Error inesperado: " + e.getMessage());
            return errorResponse;
        }
    }
}