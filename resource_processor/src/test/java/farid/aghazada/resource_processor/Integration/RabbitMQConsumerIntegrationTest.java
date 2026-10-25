package farid.aghazada.resource_processor.Integration;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import farid.aghazada.resource_processor.Service.Mp3MetadataExtractor;

/** Real broker: exchange -> queue binding -> @RabbitListener -> extractor. */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.loadbalancer.enabled=false"
})
@Testcontainers
class RabbitMQConsumerIntegrationTest {

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4-alpine");

    @DynamicPropertySource
    static void rabbitProps(DynamicPropertyRegistry r) {
        r.add("spring.rabbitmq.host", rabbit::getHost);
        r.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        r.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        r.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    RabbitTemplate rabbitTemplate;

    @MockitoBean
    Mp3MetadataExtractor extractor;

    @Test
    void messagePublishedToExchange_reachesExtractor() {
        rabbitTemplate.convertAndSend("exchange-name", "routing-key", "42");

        verify(extractor, timeout(10_000)).processResource("42");
    }
}
