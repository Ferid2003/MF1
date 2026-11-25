package farid.aghazada.resource_processor.RabbitMQ;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.processed.exchange:processed-exchange}")
    private String exchange;

    @Value("${rabbitmq.processed.routing-key:processed-routing-key}")
    private String routingKey;

    public void sendProcessed(String resourceId) {
        rabbitTemplate.convertAndSend(exchange, routingKey, resourceId);
        log.info("Published processed event for resource {}", resourceId);
    }

    @Configuration
    static class Topology {
        @Bean
        Declarables processedExchange(@Value("${rabbitmq.processed.exchange:processed-exchange}") String exchange) {
            return new Declarables(new DirectExchange(exchange));
        }
    }
}
