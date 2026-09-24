package com.example.inventory.support;

import com.example.inventory.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestRestTemplate
public abstract class IntegrationTest {

    @Autowired
    protected TestRestTemplate testRestTemplate;

    /** Idempotency-Key 헤더를 붙인 요청을 만든다. postForEntity의 요청 인자로 그대로 넘기면 된다. */
    protected static HttpEntity<Object> withIdempotencyKey(Object body, String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", key);
        return new HttpEntity<>(body, headers);
    }
}
