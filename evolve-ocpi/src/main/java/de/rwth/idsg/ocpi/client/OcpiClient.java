package de.rwth.idsg.ocpi.client;

import de.rwth.idsg.ocpi.model.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcpiClient {

    private final RestTemplate ocpiRestTemplate;

    public List<Token> getTokens(String url, String token) {
        List<Token> tokens = new ArrayList<>();
        String nextUrl = url;

        while (nextUrl != null) {
            log.info("Fetching tokens from: {}", nextUrl);
            ResponseEntity<Token[]> response = ocpiRestTemplate.exchange(
                    nextUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders(token)),
                    Token[].class
            );

            if (response.getBody() != null) {
                for (Token t : response.getBody()) {
                    tokens.add(t);
                }
            }

            nextUrl = getNextUrl(response.getHeaders());
        }

        return tokens;
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Token " + token);
        return headers;
    }

    private String getNextUrl(HttpHeaders headers) {
        // Implementation according to OCPI 2.1.1 section 8
        List<String> linkHeaders = headers.get("Link");
        if (linkHeaders == null) {
            return null;
        }

        for (String linkHeader : linkHeaders) {
            String[] parts = linkHeader.split(";");
            if (parts.length == 2 && parts[1].contains("rel=\"next\"")) {
                return parts[0].replaceAll("[<>]", "");
            }
        }

        return null;
    }
}
