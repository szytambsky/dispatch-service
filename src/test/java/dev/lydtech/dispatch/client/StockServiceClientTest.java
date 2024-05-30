package dev.lydtech.dispatch.client;

import dev.lydtech.dispatch.client.exception.RetryableException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StockServiceClientTest {

    private RestTemplate restTemplateMock;
    private StockServiceClient client;

    private static final String STOCK_SERVICE_ENDPOINT = "endpoint";
    private static final String STOCK_SERVICE_QUERY = STOCK_SERVICE_ENDPOINT + "?item=my-item";

    @BeforeEach
    void setUp() {
        restTemplateMock = Mockito.mock(RestTemplate.class);
        client = new StockServiceClient(restTemplateMock, "endpoint");
    }

    @Test
    public void testCheckAvailability_Success() {
        ResponseEntity<String> response = new ResponseEntity<>("true", HttpStatusCode.valueOf(200));
        Mockito.when(restTemplateMock.getForEntity(STOCK_SERVICE_QUERY, String.class)).thenReturn(response);
        MatcherAssert.assertThat(client.checkAvailability("my-item"), Matchers.equalTo("true"));
        verify(restTemplateMock, times(1)).getForEntity(STOCK_SERVICE_QUERY, String.class);
    }

    @Test
    public void testCheckAvailability_ServerError() {
        when(restTemplateMock.getForEntity(STOCK_SERVICE_QUERY, String.class)).thenThrow(new HttpServerErrorException(HttpStatusCode.valueOf(500)));
        assertThrows(RetryableException.class, () -> client.checkAvailability("my-item"));
        verify(restTemplateMock, times(1)).getForEntity(STOCK_SERVICE_QUERY, String.class);
    }

    @Test
    public void testCheckAvailability_ResourceAccessException() {
        when(restTemplateMock.getForEntity(STOCK_SERVICE_QUERY, String.class)).thenThrow(new ResourceAccessException("access exception"));
        assertThrows(RetryableException.class, () -> client.checkAvailability("my-item"));
        verify(restTemplateMock, times(1)).getForEntity(STOCK_SERVICE_QUERY, String.class);
    }

    @Test
    public void testCheckAvailability_RuntimeException() {
        when(restTemplateMock.getForEntity(STOCK_SERVICE_QUERY, String.class)).thenThrow(new RuntimeException("general exception"));
        assertThrows(Exception.class, () -> client.checkAvailability("my-item"));
        verify(restTemplateMock, times(1)).getForEntity(STOCK_SERVICE_QUERY, String.class);
    }
}
