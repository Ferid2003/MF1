package farid.aghazada.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.client.simple.instances.resource-service[0].uri=http://127.0.0.1:1",
                // routes live in the config server (gateway.properties), which tests don't reach
                "spring.cloud.gateway.server.webflux.routes[0].id=song-service",
                "spring.cloud.gateway.server.webflux.routes[0].uri=lb://song-service",
                "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/songs/**",
                "spring.cloud.gateway.server.webflux.routes[1].id=resource-service",
                "spring.cloud.gateway.server.webflux.routes[1].uri=lb://resource-service",
                "spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/resources/**",
                "spring.cloud.gateway.server.webflux.httpclient.connect-timeout=2000"
        })
class GlobalErrorHandlerTest {

    @LocalServerPort
    int port;

    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void undefinedRouteReturnsFriendly404() {
        client.get().uri("/unknown").exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("404")
                .jsonPath("$.errorMessage").isEqualTo("Route '/unknown' not found");
    }

    @Test
    void serviceWithNoInstancesReturnsFriendly503() {
        client.get().uri("/songs/1").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("503")
                .jsonPath("$.errorMessage").isEqualTo("Service is temporarily unavailable. Please try again later.");
    }

    @Test
    void unreachableInstanceReturnsFriendly503() {
        client.get().uri("/resources/1").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("503");
    }
}
