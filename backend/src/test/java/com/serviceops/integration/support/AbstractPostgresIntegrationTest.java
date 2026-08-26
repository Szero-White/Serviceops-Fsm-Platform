package com.serviceops.integration.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractPostgresIntegrationTest {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    @Autowired
    protected TestRestTemplate restTemplate;

    protected static PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("serviceops")
                .withUsername("serviceops")
                .withPassword("serviceops");
    }

    protected static void registerDatasource(
            DynamicPropertyRegistry registry,
            PostgreSQLContainer<?> postgres
    ) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // Integration tests use the seeded demo accounts with the fixture password below.
        // Override local/environment demo credentials so CI is deterministic.
        registry.add("serviceops.demo.seed-password", () -> "123456");
    }

    protected String login(String username, String password) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("username", username, "password", password),
                        jsonHeaders()
                ),
                MAP_RESPONSE_TYPE
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return String.valueOf(response.getBody().get("accessToken"));
    }

    protected ResponseEntity<String> exchangeGet(String path, String token) {
        return restTemplate.exchange(
                path,
                HttpMethod.GET,
                new HttpEntity<>(null, bearerHeaders(token)),
                String.class
        );
    }

    protected ResponseEntity<Map<String, Object>> exchangeGetMap(String path, String token) {
        return restTemplate.exchange(
                path,
                HttpMethod.GET,
                new HttpEntity<>(null, bearerHeaders(token)),
                MAP_RESPONSE_TYPE
        );
    }

    protected ResponseEntity<String> postJson(
            String path,
            String token,
            Map<String, Object> body
    ) {
        return restTemplate.exchange(
                path,
                HttpMethod.POST,
                new HttpEntity<>(body, authenticatedJsonHeaders(token)),
                String.class
        );
    }

    protected ResponseEntity<Map<String, Object>> postJsonMap(
            String path,
            String token,
            Map<String, Object> body
    ) {
        return restTemplate.exchange(
                path,
                HttpMethod.POST,
                new HttpEntity<>(body, authenticatedJsonHeaders(token)),
                MAP_RESPONSE_TYPE
        );
    }

    protected ResponseEntity<String> putJson(
            String path,
            String token,
            Map<String, Object> body
    ) {
        return restTemplate.exchange(
                path,
                HttpMethod.PUT,
                new HttpEntity<>(body, authenticatedJsonHeaders(token)),
                String.class
        );
    }

    protected List<Integer> runTwoConcurrentPosts(
            String path,
            String token,
            Map<String, Object> body
    ) throws Exception {
        return runConcurrentPosts(path, path, token, body);
    }

    protected List<Integer> runConcurrentPosts(
            String firstPath,
            String secondPath,
            String token,
            Map<String, Object> body
    ) throws Exception {
        return runConcurrentRequests(
                () -> postJson(firstPath, token, body).getStatusCode().value(),
                () -> postJson(secondPath, token, body).getStatusCode().value()
        );
    }

    protected List<Integer> runConcurrentRequests(
            Callable<Integer> firstRequest,
            Callable<Integer> secondRequest
    ) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> first = executor.submit(() -> {
                start.await();
                return firstRequest.call();
            });
            Future<Integer> second = executor.submit(() -> {
                start.await();
                return secondRequest.call();
            });

            start.countDown();
            return List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> mapList(Map<String, Object> body, String key) {
        Object value = body.get(key);
        assertThat(value).isInstanceOf(List.class);
        return (List<Map<String, Object>>) value;
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpHeaders authenticatedJsonHeaders(String token) {
        HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
