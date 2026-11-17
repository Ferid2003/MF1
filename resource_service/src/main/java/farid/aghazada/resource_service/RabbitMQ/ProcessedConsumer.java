package farid.aghazada.resource_service.RabbitMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import farid.aghazada.resource_service.Service.ResourceService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProcessedConsumer {

    private final ResourceService resourceService;

    @RabbitListener(queues = "${rabbitmq.processed.queue:processed-queue}")
    public void receiveProcessed(String id) {
        resourceService.moveToPermanent(Integer.valueOf(id));
    }

    @Configuration
    static class Topology {
        @Bean
        Declarables processedTopology(
                @Value("${rabbitmq.processed.exchange:processed-exchange}") String exchange,
                @Value("${rabbitmq.processed.queue:processed-queue}") String queue,
                @Value("${rabbitmq.processed.routing-key:processed-routing-key}") String routingKey) {
            DirectExchange ex = new DirectExchange(exchange);
            Queue q = new Queue(queue, true);
            Binding binding = BindingBuilder.bind(q).to(ex).with(routingKey);
            return new Declarables(ex, q, binding);
        }
    }
}
