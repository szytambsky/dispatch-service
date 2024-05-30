package dev.lydtech.dispatch.client;

import dev.lydtech.dispatch.client.exception.NotRetryableException;
import dev.lydtech.dispatch.client.exception.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class StockServiceClient {

    private final RestTemplate restTemplate;
    private final String stockServiceEndpoint;

    public StockServiceClient(@Autowired RestTemplate restTemplate,
                              @Value("${dispatch.stockServiceEndpoint}") String stockServiceEndpoint) {
        this.restTemplate = restTemplate;
        this.stockServiceEndpoint = stockServiceEndpoint;
    }

    /**
     * Stock service return true if a product is available, otherwise false;
     * **/
    public String checkAvailability(String item) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(stockServiceEndpoint + "?item=" + item, String.class);
            if (response.getStatusCode().value() != 200) {
                throw new RuntimeException("Error: " + response.getStatusCode().value());
            }
            return response.getBody();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            log.warn("Failure calling external service", e);
            throw new RetryableException(e);
        } catch (Exception e) {
            log.error("Exception thrown: {}", e.getClass().getName(), e);
            throw new NotRetryableException(e);
        }
    }
}
